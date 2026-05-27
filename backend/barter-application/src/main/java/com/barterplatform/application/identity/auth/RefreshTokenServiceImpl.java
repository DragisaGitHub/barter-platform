package com.barterplatform.application.identity.auth;

import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.RefreshTokenEntity;
import com.barterplatform.infrastructure.identity.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationDays;

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${barter.jwt.refresh-token-expiration-days}") long refreshTokenExpirationDays) {
        if (refreshTokenExpirationDays <= 0) {
            throw new IllegalStateException("barter.jwt.refresh-token-expiration-days (JWT_REFRESH_EXPIRATION_DAYS) must be greater than 0.");
        }
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    @Override
    public RefreshTokenResult createRefreshToken(Long userId) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(userId);
        entity.setTokenHash(tokenHash);
        entity.setExpiresAt(OffsetDateTime.now().plusDays(refreshTokenExpirationDays));
        entity.setCreatedAt(OffsetDateTime.now());

        RefreshTokenEntity saved = refreshTokenRepository.save(entity);
        return new RefreshTokenResult(rawToken, saved);
    }

    @Override
    public RefreshTokenEntity validateAndGet(String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshTokenEntity token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        ErrorCode.UNAUTHORIZED,
                        "Invalid refresh token."));

        if (token.getRevokedAt() != null) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCode.UNAUTHORIZED,
                    "Refresh token has been revoked.");
        }

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCode.UNAUTHORIZED,
                    "Refresh token has expired.");
        }

        return token;
    }

    @Override
    public void revoke(RefreshTokenEntity token) {
        token.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(token);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokePresentedTokenForRejectedRefresh(RefreshTokenEntity token) {
        OffsetDateTime revokedAt = OffsetDateTime.now();
        if (token.getId() != null) {
            int updated = refreshTokenRepository.revokeByIdIfActive(token.getId(), revokedAt);
            if (updated > 0) {
                token.setRevokedAt(revokedAt);
                return;
            }
        }

        if (token.getRevokedAt() == null) {
            token.setRevokedAt(revokedAt);
        }
    }

    @Override
    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationDays * 24 * 60 * 60;
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
