package com.barterplatform.application.identity.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMinutes;

    public JwtServiceImpl(
            @Value("${barter.jwt.secret}") String secret,
            @Value("${barter.jwt.access-token-expiration-minutes}") long accessTokenExpirationMinutes) {
        JwtSecretValidator.validateOrThrow(secret, "barter.jwt.secret (JWT_SECRET)");
        if (accessTokenExpirationMinutes <= 0) {
            throw new IllegalStateException("barter.jwt.access-token-expiration-minutes (JWT_ACCESS_EXPIRATION_MINUTES) must be greater than 0.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
    }

    @Override
    public String generateAccessToken(UUID userUuid, String username, List<String> roles) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(accessTokenExpirationMinutes * 60);

        return Jwts.builder()
                .subject(userUuid.toString())
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMinutes * 60;
    }

    @Override
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public UUID extractUserUuid(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    @Override
    public String extractUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        List<String> roles = parseClaims(token).get("roles", List.class);
        return roles != null ? roles : List.of();
    }
}
