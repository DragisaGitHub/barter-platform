package com.barterplatform.application.identity.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VerificationCodeHasherTest {

    @Test
    void hmacIsDeterministicAndBoundToUserAndSecret() {
        VerificationCodeHasher first = new VerificationCodeHasher("dedicated-test-secret-with-at-least-32-chars", "unused-jwt-secret");
        VerificationCodeHasher sameKey = new VerificationCodeHasher("dedicated-test-secret-with-at-least-32-chars", "another-jwt-secret");
        VerificationCodeHasher otherKey = new VerificationCodeHasher("different-test-secret-with-at-least-32-chars", "unused-jwt-secret");

        String hash = first.hash(42L, "123456");

        assertEquals(64, hash.length());
        assertTrue(first.matches(42L, "123456", hash));
        assertTrue(sameKey.matches(42L, "123456", hash));
        assertFalse(first.matches(43L, "123456", hash));
        assertFalse(first.matches(42L, "654321", hash));
        assertFalse(otherKey.matches(42L, "123456", hash));
        assertFalse(first.matches(42L, "123456", "not-hex"));
    }

    @Test
    void usesDomainSeparatedJwtSecretFallbackWhenDedicatedKeyIsEmpty() {
        VerificationCodeHasher hasher = new VerificationCodeHasher("", "jwt-test-secret-with-at-least-32-characters");
        String hash = hasher.hash(1L, "000001");

        assertTrue(hasher.matches(1L, "000001", hash));
    }

    @Test
    void rejectsShortDedicatedSecretWithoutRevealingItsValue() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new VerificationCodeHasher("short-key", "long-jwt-secret-value-for-testing"));
        assertEquals("barter.verification.code-secret must contain at least 32 printable ASCII characters.", exception.getMessage());
        assertFalse(exception.getMessage().contains("short-key"));
    }

    @Test
    void rejectsWhitespaceAndNonAsciiDedicatedSecrets() {
        assertThrows(IllegalStateException.class,
                () -> new VerificationCodeHasher("dedicated-key-with-spaces-that-is-long-enough ", "unused-jwt-secret"));
        assertThrows(IllegalStateException.class,
                () -> new VerificationCodeHasher("é".repeat(32), "unused-jwt-secret"));
    }
}
