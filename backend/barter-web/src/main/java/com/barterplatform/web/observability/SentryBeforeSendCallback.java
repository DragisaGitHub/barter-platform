package com.barterplatform.web.observability;

import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions.BeforeSendCallback;
import io.sentry.protocol.Request;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Sentry before-send hook that enforces privacy constraints before any event leaves the JVM:
 * <ul>
 *   <li>Removes sensitive request headers (Authorization, Cookie, token/key/secret/password variants).</li>
 *   <li>Clears request body ({@code data}) and cookie string.</li>
 *   <li>Clears the Sentry {@code User} object to prevent PII (email, username, IP) from being sent.</li>
 *   <li>Attaches the current {@code correlationId} from MDC as a Sentry tag for traceability.</li>
 * </ul>
 *
 * <p>Registered automatically by Sentry Spring Boot auto-configuration when present as a Spring bean.
 * This bean is a no-op when {@code SENTRY_DSN_BACKEND} is not set (Sentry is disabled).
 */
@Component
public class SentryBeforeSendCallback implements BeforeSendCallback {

    // Headers that must never be forwarded to Sentry.
    private static final Set<String> BLOCKED_HEADER_NAMES = Set.of(
            "authorization",
            "cookie",
            "x-auth-token",
            "x-api-key",
            "x-access-token",
            "x-refresh-token"
    );

    @Override
    public SentryEvent execute(SentryEvent event, Hint hint) {
        scrubRequest(event);
        clearUser(event);
        attachCorrelationId(event);
        return event;
    }

    private void scrubRequest(SentryEvent event) {
        Request request = event.getRequest();
        if (request == null) {
            return;
        }
        // Never send request or response body
        request.setData(null);
        // Never send cookie header values
        request.setCookies(null);
        // Scrub sensitive headers
        Map<String, String> headers = request.getHeaders();
        if (headers != null && !headers.isEmpty()) {
            Map<String, String> scrubbed = new LinkedHashMap<>(headers);
            scrubbed.entrySet().removeIf(entry -> isSensitiveHeader(entry.getKey()));
            request.setHeaders(scrubbed);
        }
    }

    /**
     * Returns {@code true} when the header name matches any blocked name or contains
     * a sensitive keyword (token, secret, password, key).
     */
    boolean isSensitiveHeader(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (BLOCKED_HEADER_NAMES.contains(lower)) {
            return true;
        }
        return lower.contains("token")
                || lower.contains("secret")
                || lower.contains("password")
                || lower.contains("key");
    }

    private void clearUser(SentryEvent event) {
        // Drop the entire User context to prevent email, username and IP from being sent.
        event.setUser(null);
    }

    private void attachCorrelationId(SentryEvent event) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        if (correlationId != null && !correlationId.isBlank()) {
            event.setTag("correlationId", correlationId);
        }
    }
}

