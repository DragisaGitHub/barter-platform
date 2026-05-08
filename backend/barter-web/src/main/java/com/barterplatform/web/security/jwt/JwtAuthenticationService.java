package com.barterplatform.web.security.jwt;

import com.barterplatform.application.identity.auth.JwtService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class JwtAuthenticationService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public Authentication authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());

        if (!jwtService.validateToken(token)) {
            return null;
        }

        UUID userUuid = jwtService.extractUserUuid(token);
        String username = jwtService.extractUsername(token);
        List<String> roles = jwtService.extractRoles(token);

        AuthenticatedUser principal = new AuthenticatedUser(userUuid, username, roles);

        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
