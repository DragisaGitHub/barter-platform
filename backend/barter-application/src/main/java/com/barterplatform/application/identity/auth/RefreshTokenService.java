package com.barterplatform.application.identity.auth;

import com.barterplatform.domain.identity.entity.RefreshTokenEntity;

public interface RefreshTokenService {

    /**
     * Generates a secure random refresh token, persists the hashed version, and returns the raw token.
     */
    RefreshTokenResult createRefreshToken(Long userId);

    /**
     * Validates and returns the refresh token entity for the given raw token.
     * Throws if not found, expired, or revoked.
     */
    RefreshTokenEntity validateAndGet(String rawToken);

    /**
     * Revokes the refresh token by setting revoked_at.
     */
    void revoke(RefreshTokenEntity token);

    long getRefreshTokenExpirationSeconds();

    record RefreshTokenResult(String rawToken, RefreshTokenEntity entity) {}
}
