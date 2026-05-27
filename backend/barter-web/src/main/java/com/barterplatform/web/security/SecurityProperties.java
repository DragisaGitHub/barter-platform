package com.barterplatform.web.security;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "barter.security")
public class SecurityProperties {

    private List<String> allowedOrigins = new ArrayList<>();
    private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    private List<String> allowedHeaders = new ArrayList<>(List.of(
            "Accept",
            "Authorization",
            "Content-Type",
            "Origin",
            "X-Correlation-Id",
            "X-Request-Id",
            "X-Requested-With"));
    private List<String> exposedHeaders = new ArrayList<>(List.of("X-Correlation-Id"));
    @Setter
    @Getter
    private boolean allowCredentials = false;
    @Setter
    @Getter
    private Duration corsMaxAge = Duration.ofHours(1);
    @Setter
    @Getter
    private boolean swaggerEnabled = true;

    public List<String> getAllowedOrigins() {
        return normalizeValues(allowedOrigins).stream()
                .map(this::stripTrailingSlash)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> getAllowedMethods() {
        return normalizeValues(allowedMethods).stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> getAllowedHeaders() {
        return normalizeValues(allowedHeaders);
    }

    public List<String> getExposedHeaders() {
        return normalizeValues(exposedHeaders);
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? new ArrayList<>() : new ArrayList<>(allowedOrigins);
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods == null ? new ArrayList<>() : new ArrayList<>(allowedMethods);
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders == null ? new ArrayList<>() : new ArrayList<>(allowedHeaders);
    }

    public void setExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders == null ? new ArrayList<>() : new ArrayList<>(exposedHeaders);
    }

    private List<String> normalizeValues(List<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    private String stripTrailingSlash(String origin) {
        if (origin.endsWith("/")) {
            return origin.substring(0, origin.length() - 1);
        }
        return origin;
    }
}

