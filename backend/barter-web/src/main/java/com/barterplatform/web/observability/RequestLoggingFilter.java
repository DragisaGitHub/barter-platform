package com.barterplatform.web.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            String path = request.getRequestURI();
            int status = response.getStatus();

            if (isQuietHealthEndpoint(path) && status < 400) {
                log.debug("HTTP request completed: method={}, path={}, status={}, durationMs={}",
                        request.getMethod(), path, status, durationMs);
                return;
            }

            if (status >= 500) {
                log.error("HTTP request completed: method={}, path={}, status={}, durationMs={}",
                        request.getMethod(), path, status, durationMs);
                return;
            }

            if (status >= 400) {
                log.warn("HTTP request completed: method={}, path={}, status={}, durationMs={}",
                        request.getMethod(), path, status, durationMs);
                return;
            }

            log.info("HTTP request completed: method={}, path={}, status={}, durationMs={}",
                    request.getMethod(), path, status, durationMs);
        }
    }

    private boolean isQuietHealthEndpoint(String path) {
        return "/actuator/health".equals(path)
                || path.startsWith("/actuator/health/")
                || "/ping".equals(path)
                || "/api/v1/ping".equals(path);
    }
}

