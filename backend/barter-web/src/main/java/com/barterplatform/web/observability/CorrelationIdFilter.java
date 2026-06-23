package com.barterplatform.web.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String CORRELATION_ID_REQUEST_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".correlationId";

    private static final Pattern SAFE_HEADER_VALUE = Pattern.compile("^[A-Za-z0-9._-]{8,128}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);

        request.setAttribute(CORRELATION_ID_REQUEST_ATTRIBUTE, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader(REQUEST_ID_HEADER, correlationId);
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String existingCorrelationId = sanitize(request.getHeader(CORRELATION_ID_HEADER));
        if (existingCorrelationId != null) {
            return existingCorrelationId;
        }

        String existingRequestId = sanitize(request.getHeader(REQUEST_ID_HEADER));
        if (existingRequestId != null) {
            return existingRequestId;
        }

        return UUID.randomUUID().toString();
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (SAFE_HEADER_VALUE.matcher(trimmed).matches()) {
            return trimmed;
        }

        return null;
    }
}

