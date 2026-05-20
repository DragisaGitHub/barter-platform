package com.barterplatform.web.ratelimit;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

final class RateLimitRule {

    private final String id;
    private final String method;
    private final Pattern pathPattern;
    private final boolean authenticatedUserPreferred;
    private final Function<RateLimitProperties, RateLimitProperties.Policy> policyResolver;

    RateLimitRule(String id,
                  String method,
                  String pathRegex,
                  boolean authenticatedUserPreferred,
                  Function<RateLimitProperties, RateLimitProperties.Policy> policyResolver) {
        this.id = id;
        this.method = method.toUpperCase(Locale.ROOT);
        this.pathPattern = Pattern.compile(pathRegex);
        this.authenticatedUserPreferred = authenticatedUserPreferred;
        this.policyResolver = policyResolver;
    }

    String id() {
        return id;
    }

    boolean authenticatedUserPreferred() {
        return authenticatedUserPreferred;
    }

    RateLimitProperties.Policy policy(RateLimitProperties properties) {
        return policyResolver.apply(properties);
    }

    boolean matches(String requestMethod, String normalizedPath) {
        return method.equalsIgnoreCase(requestMethod) && pathPattern.matcher(normalizedPath).matches();
    }

    static Optional<RateLimitRule> findMatching(String method, String normalizedPath, Iterable<RateLimitRule> rules) {
        for (RateLimitRule rule : rules) {
            if (rule.matches(method, normalizedPath)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }
}

