package com.barterplatform.application.identity.auth;

import java.util.Locale;
import java.util.Set;

public final class JwtSecretValidator {

    public static final int MIN_SECRET_LENGTH = 32;

    private static final Set<String> INSECURE_JWT_SECRETS = Set.of(
            "default-dev-secret-key-that-is-at-least-32-bytes-long!!",
            "replace-with-long-random-dev-secret-at-least-32-bytes",
            "replace-with-long-random-production-secret",
            "changeme",
            "change-me",
            "secret",
            "jwt-secret");

    private JwtSecretValidator() {
    }

    public static void validateOrThrow(String secret, String propertyName) {
        String normalizedSecret = secret == null ? "" : secret.trim();
        String normalizedLowerCase = normalizedSecret.toLowerCase(Locale.ROOT);

        if (normalizedSecret.isBlank()) {
            throw new IllegalStateException(propertyName + " is required and must not be blank.");
        }

        if (normalizedSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(propertyName + " must be at least " + MIN_SECRET_LENGTH + " characters long.");
        }

        if (INSECURE_JWT_SECRETS.contains(normalizedLowerCase)
                || normalizedLowerCase.contains("default-dev-secret")
                || normalizedLowerCase.contains("replace-with-long-random-dev-secret")
                || normalizedLowerCase.contains("replace-with-long-random-production-secret")) {
            throw new IllegalStateException(propertyName + " must not use a placeholder or known unsafe default value.");
        }
    }
}
