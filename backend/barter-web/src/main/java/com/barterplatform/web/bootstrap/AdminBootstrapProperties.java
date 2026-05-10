package com.barterplatform.web.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "barter.bootstrap.admin")
public record AdminBootstrapProperties(
        boolean enabled,
        String username,
        String email,
        String password
) {
}

