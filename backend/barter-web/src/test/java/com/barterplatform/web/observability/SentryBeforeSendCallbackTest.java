package com.barterplatform.web.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.protocol.Request;
import io.sentry.protocol.User;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class SentryBeforeSendCallbackTest {

    private SentryBeforeSendCallback callback;

    @BeforeEach
    void setUp() {
        callback = new SentryBeforeSendCallback();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    // -----------------------------------------------------------------------
    // Request header scrubbing
    // -----------------------------------------------------------------------

    @Test
    void shouldRemoveAuthorizationHeader() {
        SentryEvent event = eventWithHeaders(Map.of("Authorization", "Bearer secret-token"));

        SentryEvent result = callback.execute(event, new Hint());

        assertThat(result.getRequest().getHeaders()).doesNotContainKey("Authorization");
    }

    @Test
    void shouldRemoveCookieHeader() {
        SentryEvent event = eventWithHeaders(Map.of("Cookie", "session=abc123"));

        SentryEvent result = callback.execute(event, new Hint());

        assertThat(result.getRequest().getHeaders()).doesNotContainKey("Cookie");
    }

    @Test
    void shouldRemoveHeadersContainingTokenKeyword() {
        SentryEvent event = eventWithHeaders(Map.of(
                "X-Access-Token", "some-token",
                "X-Refresh-Token", "some-refresh",
                "x-custom-token", "another"
        ));

        SentryEvent result = callback.execute(event, new Hint());

        assertThat(result.getRequest().getHeaders()).isEmpty();
    }

    @Test
    void shouldRemoveHeadersContainingKeyKeyword() {
        SentryEvent event = eventWithHeaders(Map.of("X-Api-Key", "secret-key"));

        SentryEvent result = callback.execute(event, new Hint());

        assertThat(result.getRequest().getHeaders()).doesNotContainKey("X-Api-Key");
    }

    @Test
    void shouldPreserveSafeHeaders() {
        SentryEvent event = eventWithHeaders(Map.of(
                "Content-Type", "application/json",
                "X-Correlation-Id", "abc-123",
                "Accept", "application/json"
        ));

        SentryEvent result = callback.execute(event, new Hint());

        assertThat(result.getRequest().getHeaders())
                .containsKey("Content-Type")
                .containsKey("X-Correlation-Id")
                .containsKey("Accept");
    }

    @Test
    void shouldTolerateNullHeaders() {
        SentryEvent event = new SentryEvent();
        Request request = new Request();
        request.setHeaders(null);
        event.setRequest(request);

        SentryEvent result = callback.execute(event, new Hint());

        assertThat(result).isNotNull();
        assertThat(result.getRequest().getHeaders()).isNull();
    }

    // -----------------------------------------------------------------------
    // Request body and cookie scrubbing
    // -----------------------------------------------------------------------

    @Test
    void shouldClearRequestBody() {
        SentryEvent event = new SentryEvent();
        Request request = new Request();
        request.setData(Map.of("password", "s3cr3t", "email", "user@example.com"));
        event.setRequest(request);

        SentryEvent result = callback.execute(event, new Hint());

        assertThat(result.getRequest().getData()).isNull();
    }

    @Test
    void shouldClearRequestCookies() {
        SentryEvent event = new SentryEvent();
        Request request = new Request();
        request.setCookies("session=abc; csrf=def");
        event.setRequest(request);

        SentryEvent result = callback.execute(event, new Hint());

        assertThat(result.getRequest().getCookies()).isNull();
    }

    // -----------------------------------------------------------------------
    // User PII
    // -----------------------------------------------------------------------

    @Test
    void shouldClearUserPii() {
        SentryEvent event = new SentryEvent();
        User user = new User();
        user.setEmail("user@example.com");
        user.setUsername("john.doe");
        event.setUser(user);

        SentryEvent result = callback.execute(event, new Hint());

        assertThat(result.getUser()).isNull();
    }

    // -----------------------------------------------------------------------
    // correlationId attachment
    // -----------------------------------------------------------------------

    @Test
    void shouldAttachCorrelationIdFromMdc() {
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, "req-abc-123456789");

        SentryEvent event = new SentryEvent();
        SentryEvent result = callback.execute(event, new Hint());

        assertThat(result.getTags()).containsEntry("correlationId", "req-abc-123456789");
    }

    @Test
    void shouldNotSetCorrelationIdTagWhenMdcIsEmpty() {
        // MDC has no correlationId set
        SentryEvent event = new SentryEvent();
        SentryEvent result = callback.execute(event, new Hint());

        Map<String, String> tags = result.getTags();
        boolean hasCorrelationId = tags != null && tags.containsKey("correlationId");
        assertThat(hasCorrelationId).isFalse();
    }

    @Test
    void shouldTolerateNullRequest() {
        SentryEvent event = new SentryEvent();
        // No request set — must not throw

        SentryEvent result = callback.execute(event, new Hint());

        assertThat(result).isNotNull();
    }

    // -----------------------------------------------------------------------
    // isSensitiveHeader helper — direct unit tests for boundary cases
    // -----------------------------------------------------------------------

    @Test
    void isSensitiveHeaderReturnsFalseForNull() {
        assertThat(callback.isSensitiveHeader(null)).isFalse();
    }

    @Test
    void isSensitiveHeaderReturnsTrueForAuthorizationCaseInsensitive() {
        assertThat(callback.isSensitiveHeader("AUTHORIZATION")).isTrue();
        assertThat(callback.isSensitiveHeader("authorization")).isTrue();
        assertThat(callback.isSensitiveHeader("Authorization")).isTrue();
    }

    @Test
    void isSensitiveHeaderReturnsTrueForCookieCaseInsensitive() {
        assertThat(callback.isSensitiveHeader("COOKIE")).isTrue();
    }

    @Test
    void isSensitiveHeaderReturnsFalseForContentType() {
        assertThat(callback.isSensitiveHeader("Content-Type")).isFalse();
    }

    // -----------------------------------------------------------------------
    // Structural guarantee: 4xx handlers do NOT call Sentry.captureException
    // -----------------------------------------------------------------------

    /**
     * Sentry.captureException is called ONLY from
     * {@link com.barterplatform.web.exception.GlobalExceptionHandler#handleGenericException},
     * which maps {@link Exception} to HTTP 500.  All 4xx handler methods
     * (handleApiException, handleMethodArgumentNotValid, handleConstraintViolation,
     * handleTypeMismatch, handleHttpMessageNotReadable, handleAccessDenied,
     * handleAuthentication) never invoke Sentry, making non-capture of 4xx a
     * compile-time structural guarantee rather than a runtime assertion.
     *
     * <p>This test asserts the callback itself does not suppress or alter events
     * (returns the same event instance) so the guarantee above is not undermined
     * at the callback level.
     */
    @Test
    void callbackAlwaysReturnsEventUnchangedInIdentity() {
        SentryEvent event = new SentryEvent();
        SentryEvent result = callback.execute(event, new Hint());
        assertThat(result).isSameAs(event);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private SentryEvent eventWithHeaders(Map<String, String> headers) {
        SentryEvent event = new SentryEvent();
        Request request = new Request();
        request.setHeaders(new LinkedHashMap<>(headers));
        event.setRequest(request);
        return event;
    }
}

