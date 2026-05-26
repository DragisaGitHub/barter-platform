package com.barterplatform.web.security;

import com.barterplatform.application.identity.auth.JwtSecretValidator;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class ProductionSecurityEnvironmentValidator implements SmartInitializingSingleton {

    private final Environment environment;
    private final SecurityProperties securityProperties;

    public ProductionSecurityEnvironmentValidator(Environment environment, SecurityProperties securityProperties) {
        this.environment = environment;
        this.securityProperties = securityProperties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!environment.matchesProfiles(String.valueOf(Profiles.of("prod")))) {
            return;
        }

        List<String> errors = new ArrayList<>();
        validateProfiles(errors);
        validateJwtSecret(errors);
        validateJwtExpirations(errors);
        validateAllowedOrigins(errors);
        validateSwagger(errors);
        validateEmailVerification(errors);
        validateStorage(errors);

        if (!errors.isEmpty()) {
            throw new ApplicationContextException("Unsafe production configuration:\n - " + String.join("\n - ", errors));
        }
    }

    private void validateProfiles(List<String> errors) {
        if (environment.matchesProfiles(String.valueOf(Profiles.of("dev"))) || environment.matchesProfiles(String.valueOf(Profiles.of("local")))) {
            errors.add("The prod profile must not be combined with dev/local profiles.");
        }
    }

    private void validateJwtSecret(List<String> errors) {
        String jwtSecret = environment.getProperty("barter.jwt.secret", "");

        try {
            JwtSecretValidator.validateOrThrow(jwtSecret, "barter.jwt.secret (JWT_SECRET)");
        } catch (IllegalStateException ex) {
            errors.add(ex.getMessage());
        }
    }

    private void validateJwtExpirations(List<String> errors) {
        Long accessTokenMinutes = environment.getProperty("barter.jwt.access-token-expiration-minutes", Long.class);
        Long refreshTokenDays = environment.getProperty("barter.jwt.refresh-token-expiration-days", Long.class);

        if (accessTokenMinutes == null || accessTokenMinutes <= 0) {
            errors.add("barter.jwt.access-token-expiration-minutes (JWT_ACCESS_EXPIRATION_MINUTES) must be greater than 0 in prod.");
        }

        if (refreshTokenDays == null || refreshTokenDays <= 0) {
            errors.add("barter.jwt.refresh-token-expiration-days (JWT_REFRESH_EXPIRATION_DAYS) must be greater than 0 in prod.");
        }
    }

    private void validateAllowedOrigins(List<String> errors) {
        for (String origin : securityProperties.getAllowedOrigins()) {
            if ("*".equals(origin)) {
                errors.add("barter.security.allowed-origins must not contain '*'. Prefer same-origin proxying or an explicit HTTPS allowlist.");
                continue;
            }

            try {
                URI uri = URI.create(origin);
                String host = uri.getHost();
                String scheme = uri.getScheme();
                if (scheme == null || !scheme.equalsIgnoreCase("https")) {
                    errors.add("barter.security.allowed-origins must use HTTPS in prod: " + origin);
                }
                if (host == null || host.isBlank()) {
                    errors.add("barter.security.allowed-origins contains an invalid origin: " + origin);
                } else if (host.equalsIgnoreCase("localhost") || host.startsWith("127.")) {
                    errors.add("barter.security.allowed-origins must not point to localhost in prod: " + origin);
                }
            } catch (IllegalArgumentException ex) {
                errors.add("barter.security.allowed-origins contains an invalid origin: " + origin);
            }
        }
    }

    private void validateSwagger(List<String> errors) {
        if (securityProperties.isSwaggerEnabled()) {
            errors.add("Swagger/OpenAPI must be disabled in prod (set BARTER_SWAGGER_ENABLED=false).");
        }
    }

    private void validateEmailVerification(List<String> errors) {
        boolean emailVerificationEnabled = environment.getProperty(
                "barter.email-verification.enabled",
                Boolean.class,
                true);
        String mailHost = environment.getProperty("spring.mail.host", "").trim();

        if (!emailVerificationEnabled) {
            errors.add("Email verification must stay enabled in prod.");
        }
        if (mailHost.isBlank()) {
            errors.add("spring.mail.host (SPRING_MAIL_HOST / SMTP_HOST) is required in prod when email verification is enabled.");
        }
    }

    private void validateStorage(List<String> errors) {
        String storageType = property("barter.storage.type", "storage.type");
        String connectionString = property("barter.storage.azure.connection-string", "azure.storage.connection-string");
        String containerName = property("barter.storage.azure.container-name", "azure.storage.container-name");

        if (!"azure".equalsIgnoreCase(storageType)) {
            errors.add("barter.storage.type must be set to 'azure' in prod.");
        }
        if (connectionString.isBlank()) {
            errors.add("barter.storage.azure.connection-string (AZURE_STORAGE_CONNECTION_STRING_PROD or AZURE_STORAGE_CONNECTION_STRING) is required in prod.");
        }
        if (containerName.isBlank()) {
            errors.add("barter.storage.azure.container-name (AZURE_STORAGE_CONTAINER_PROD or AZURE_STORAGE_CONTAINER) is required in prod.");
        }
    }

    private String property(String... keys) {
        for (String key : keys) {
            String value = environment.getProperty(key);
            if (value != null && !value.trim().isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}

