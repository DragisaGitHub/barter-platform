package com.barterplatform.web.observability;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        request = new MockHttpServletRequest("GET", "/api/v1/ping");
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        org.slf4j.MDC.clear();
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() throws ServletException, IOException {
        filter.doFilterInternal(request, response, (req, res) -> {
            String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
            assertThat(correlationId).isNotBlank();
            assertThat(org.slf4j.MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isEqualTo(correlationId);
            assertThat(request.getAttribute(CorrelationIdFilter.CORRELATION_ID_REQUEST_ATTRIBUTE)).isEqualTo(correlationId);
        });

        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(UUID.fromString(correlationId)).isNotNull();
        assertThat(org.slf4j.MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    void shouldReuseIncomingCorrelationIdWhenItIsSafe() throws ServletException, IOException {
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "client-request-1234");

        filter.doFilterInternal(request, response, (req, res) ->
                assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("client-request-1234"));

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("client-request-1234");
    }

    @Test
    void shouldFallbackToSafeGeneratedIdForMalformedIncomingHeader() throws ServletException, IOException {
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "  invalid value with spaces  ");
        request.addHeader(CorrelationIdFilter.REQUEST_ID_HEADER, "proxy-request-5678");

        filter.doFilterInternal(request, response, (req, res) ->
                assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("proxy-request-5678"));
    }
}

