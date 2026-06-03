package com.barterplatform.web.ratelimit;

import com.barterplatform.api.model.ErrorResponse;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final String API_PREFIX = "/api/v1";

    private final RateLimitProperties properties;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;
    private final List<RateLimitRule> rules;

    public RateLimitingFilter(RateLimitProperties properties,
                              RateLimitService rateLimitService,
                              ObjectMapper objectMapper) {
        this.properties = properties;
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
        this.rules = List.of(
                new RateLimitRule("auth-login", "POST", "^/auth/login$", false, RateLimitProperties::getLogin),
                new RateLimitRule("auth-register", "POST", "^/auth/register$", false, RateLimitProperties::getRegister),
                new RateLimitRule("auth-refresh", "POST", "^/auth/refresh$", false, RateLimitProperties::getRefreshToken),
                new RateLimitRule("auth-forgot-password", "POST", "^/auth/forgot-password$", false, RateLimitProperties::getForgotPassword),
                new RateLimitRule("auth-reset-password", "POST", "^/auth/reset-password$", false, RateLimitProperties::getResetPassword),
                new RateLimitRule("auth-resend-verification-code", "POST", "^/auth/resend-verification-code$", false,
                        RateLimitProperties::getResendVerificationCode),
                new RateLimitRule("image-upload", "POST", "^/catalog/items/[^/]+/images$", true,
                        RateLimitProperties::getImageUpload),
                new RateLimitRule("trade-offer-create", "POST", "^/trade-offers$", true,
                        RateLimitProperties::getTradeOfferCreate),
                new RateLimitRule("trade-message-send", "POST", "^/trade-offers/[^/]+/messages$", true,
                        RateLimitProperties::getTradeMessageSend),
                new RateLimitRule("beta-feedback-submit", "POST", "^/feedback/beta$", true,
                        RateLimitProperties::getBetaFeedback),
                new RateLimitRule("favorite-add", "POST", "^/catalog/items/[^/]+/favorite$", true,
                        RateLimitProperties::getFavoriteMutation),
                new RateLimitRule("favorite-remove", "DELETE", "^/catalog/items/[^/]+/favorite$", true,
                        RateLimitProperties::getFavoriteMutation)
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled() || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String normalizedPath = normalizePath(request);
        Optional<RateLimitRule> matchingRule = RateLimitRule.findMatching(request.getMethod(), normalizedPath, rules);
        if (matchingRule.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitRule rule = matchingRule.get();
        String key = resolveKey(request, rule);
        RateLimitService.RateLimitDecision decision = rateLimitService.tryAcquire(rule.id(), key, rule.policy(properties));
        if (decision.permitted()) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit exceeded: rule={}, method={}, path={}, retryAfterSeconds={}",
                rule.id(), request.getMethod(), normalizedPath, decision.retryAfterSeconds());
        writeTooManyRequestsResponse(request, response, decision.retryAfterSeconds());
    }

    private String normalizePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        if (path.startsWith(API_PREFIX + "/") || path.equals(API_PREFIX)) {
            path = path.substring(API_PREFIX.length());
        }

        return path.isBlank() ? "/" : path;
    }

    private String resolveKey(HttpServletRequest request, RateLimitRule rule) {
        String clientIp = resolveClientIp(request);
        if (rule.authenticatedUserPreferred()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
                return "user:" + user.getUserUuid() + ":ip:" + clientIp;
            }
        }
        return "ip:" + clientIp;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String configuredHeader = properties.getClientIpHeader();
        if (configuredHeader != null && !configuredHeader.isBlank()) {
            String headerValue = request.getHeader(configuredHeader);
            if (headerValue != null && !headerValue.isBlank()) {
                return headerValue.split(",")[0].trim().toLowerCase(Locale.ROOT);
            }
        }

        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
    }

    private void writeTooManyRequestsResponse(HttpServletRequest request,
                                              HttpServletResponse response,
                                              long retryAfterSeconds) throws IOException {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        response.setStatus(status.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = new ErrorResponse()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(ErrorCode.RATE_LIMITED.name())
                .message("Too many requests. Please retry later.")
                .path(request.getRequestURI())
                .fieldErrors(new ArrayList<>());

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}

