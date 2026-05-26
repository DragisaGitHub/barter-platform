package com.barterplatform.application.identity.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceImplTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long!!";
    private static final long EXPIRATION_MINUTES = 15;

    private JwtServiceImpl jwtService;
    private SecretKey verificationKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl(SECRET, EXPIRATION_MINUTES);
        verificationKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldGenerateValidAccessToken() {
        UUID userUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String username = "alex99";
        List<String> roles = List.of("USER", "ADMIN");

        String token = jwtService.generateAccessToken(userUuid, username, roles);

        assertNotNull(token);
        assertFalse(token.isBlank());

        Claims claims = Jwts.parser()
                .verifyWith(verificationKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(userUuid.toString(), claims.getSubject());
        assertEquals(username, claims.get("username", String.class));

        @SuppressWarnings("unchecked")
        List<String> tokenRoles = claims.get("roles", List.class);
        assertEquals(roles, tokenRoles);

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    void shouldSetCorrectExpirationTime() {
        UUID userUuid = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userUuid, "user1", List.of("USER"));

        Claims claims = Jwts.parser()
                .verifyWith(verificationKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        long diffSeconds = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000;
        assertEquals(EXPIRATION_MINUTES * 60, diffSeconds);
    }

    @Test
    void shouldReturnCorrectExpirationSeconds() {
        assertEquals(EXPIRATION_MINUTES * 60, jwtService.getAccessTokenExpirationSeconds());
    }

    @Test
    void shouldGenerateUniqueTokensForDifferentUsers() {
        String token1 = jwtService.generateAccessToken(UUID.randomUUID(), "user1", List.of("USER"));
        String token2 = jwtService.generateAccessToken(UUID.randomUUID(), "user2", List.of("USER"));

        assertFalse(token1.equals(token2));
    }

    @Test
    void shouldHandleEmptyRolesList() {
        UUID userUuid = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userUuid, "user1", List.of());

        Claims claims = Jwts.parser()
                .verifyWith(verificationKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        @SuppressWarnings("unchecked")
        List<String> tokenRoles = claims.get("roles", List.class);
        assertTrue(tokenRoles.isEmpty());
    }

    @Test
    void shouldOnlyIncludeMinimalExpectedClaims() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "alex99", List.of("USER"));

        Claims claims = Jwts.parser()
                .verifyWith(verificationKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(Set.of("sub", "username", "roles", "iat", "exp"), claims.keySet());
    }

    @Test
    void shouldFailFastWhenSecretIsBlank() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new JwtServiceImpl("   ", EXPIRATION_MINUTES));

        assertEquals("barter.jwt.secret (JWT_SECRET) is required and must not be blank.", ex.getMessage());
    }

    @Test
    void shouldFailFastWhenSecretIsKnownPlaceholder() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new JwtServiceImpl("replace-with-long-random-production-secret", EXPIRATION_MINUTES));

        assertEquals("barter.jwt.secret (JWT_SECRET) must not use a placeholder or known unsafe default value.", ex.getMessage());
    }

    @Test
    void shouldFailFastWhenSecretIsTooShort() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new JwtServiceImpl("too-short-secret", EXPIRATION_MINUTES));

        assertEquals("barter.jwt.secret (JWT_SECRET) must be at least 32 characters long.", ex.getMessage());
    }

    @Test
    void shouldFailFastWhenAccessTokenExpirationIsNotPositive() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new JwtServiceImpl(SECRET, 0));

        assertEquals("barter.jwt.access-token-expiration-minutes (JWT_ACCESS_EXPIRATION_MINUTES) must be greater than 0.", ex.getMessage());
    }
}

