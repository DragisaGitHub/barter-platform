package com.barterplatform.application.identity.auth;

import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.UUID;

public interface JwtService {

    String generateAccessToken(UUID userUuid, String username, List<String> roles);

    long getAccessTokenExpirationSeconds();

    boolean validateToken(String token);

    Claims parseClaims(String token);

    UUID extractUserUuid(String token);

    String extractUsername(String token);

    List<String> extractRoles(String token);
}
