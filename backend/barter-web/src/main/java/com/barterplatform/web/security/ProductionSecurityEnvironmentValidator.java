package com.barterplatform.web.security;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class ProductionSecurityEnvironmentValidator implements SmartInitializingSingleton {

    private static final int MIN_JWT_SECRET_LENGTH = 32;
    private static final Set<String> INSECURE_JWT_SECRETS = Set.of(
            "default-dev-secret-key-that-is-at-least-32-bytes-long!!",
            "replace-with-long-random-dev-secret-at-least-32-bytes",
            "changeme",
            "change-me",
            "secret",
            "jwt-secret");

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
        String jwtSecret = environment.getProperty("barter.jwt.secret", "").trim();
        String normalized = jwtSecret.toLowerCase(Locale.ROOT);

        if (jwtSecret.isBlank()) {
            errors.add("barter.jwt.secret (JWT_SECRET) is required in prod.");
            return;
        }
        if (jwtSecret.length() < MIN_JWT_SECRET_LENGTH) {
            errors.add("barter.jwt.secret (JWT_SECRET) must be at least 32 characters long in prod.");
        }
        if (INSECURE_JWT_SECRETS.contains(normalized)
                || normalized.contains("default-dev-secret")
                || normalized.contains("replace-with-long-random-dev-secret")) {
            errors.add("barter.jwt.secret (JWT_SECRET) must not use a placeholder or known DEV default in prod.");
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
        String storageType = environment.getProperty("storage.type", "").trim();
        String connectionString = environment.getProperty("azure.storage.connection-string", "").trim();
        String containerName = environment.getProperty("azure.storage.container-name", "").trim();

        if (!"azure".equalsIgnoreCase(storageType)) {
            errors.add("storage.type must be set to 'azure' in prod.");
        }
        if (connectionString.isBlank()) {
            errors.add("azure.storage.connection-string (AZURE_STORAGE_CONNECTION_STRING) is required in prod.");
        }
        if (containerName.isBlank()) {
            errors.add("azure.storage.container-name (AZURE_STORAGE_CONTAINER) is required in prod.");
        }
    }
}

