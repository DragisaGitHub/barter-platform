package com.barterplatform.web.security;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "barter.security")
public class SecurityProperties {

    private List<String> allowedOrigins = new ArrayList<>();
    @Setter
    @Getter
    private boolean swaggerEnabled = true;

    public List<String> getAllowedOrigins() {
        return allowedOrigins.stream()
                .filter(origin -> origin != null && !origin.isBlank())
                .map(String::trim)
                .map(this::stripTrailingSlash)
                .distinct()
                .collect(Collectors.toList());
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? new ArrayList<>() : new ArrayList<>(allowedOrigins);
    }

    private String stripTrailingSlash(String origin) {
        if (origin.endsWith("/")) {
            return origin.substring(0, origin.length() - 1);
        }
        return origin;
    }
}

