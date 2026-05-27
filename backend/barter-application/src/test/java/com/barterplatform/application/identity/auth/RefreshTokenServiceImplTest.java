package com.barterplatform.application.identity.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.RefreshTokenEntity;
import com.barterplatform.infrastructure.identity.repository.RefreshTokenRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenServiceImpl refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, 7L);
    }

    @Test
    void shouldCreateRefreshTokenAndPersistHash() {
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.RefreshTokenResult result = refreshTokenService.createRefreshToken(101L);

        assertNotNull(result.rawToken());
        assertNotNull(result.entity());
        assertEquals(101L, result.entity().getUserId());
        assertNotNull(result.entity().getTokenHash());
        assertNotNull(result.entity().getExpiresAt());
        assertNotNull(result.entity().getCreatedAt());

        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
    }

    @Test
    void shouldValidateAndReturnTokenSuccessfully() {
        String rawToken = "test-token";
        String hash = RefreshTokenServiceImpl.hashToken(rawToken);

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setTokenHash(hash);
        entity.setExpiresAt(OffsetDateTime.now().plusDays(1));
        entity.setRevokedAt(null);

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(entity));

        RefreshTokenEntity result = refreshTokenService.validateAndGet(rawToken);

        assertEquals(entity, result);
    }

    @Test
    void shouldThrowWhenTokenNotFound() {
        String rawToken = "unknown-token";
        String hash = RefreshTokenServiceImpl.hashToken(rawToken);

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> refreshTokenService.validateAndGet(rawToken));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
        assertEquals("Invalid refresh token.", ex.getMessage());
    }

    @Test
    void shouldThrowWhenTokenIsRevoked() {
        String rawToken = "revoked-token";
        String hash = RefreshTokenServiceImpl.hashToken(rawToken);

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setTokenHash(hash);
        entity.setExpiresAt(OffsetDateTime.now().plusDays(1));
        entity.setRevokedAt(OffsetDateTime.now().minusHours(1));

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(entity));

        ApiException ex = assertThrows(ApiException.class,
                () -> refreshTokenService.validateAndGet(rawToken));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Refresh token has been revoked.", ex.getMessage());
    }

    @Test
    void shouldThrowWhenTokenIsExpired() {
        String rawToken = "expired-token";
        String hash = RefreshTokenServiceImpl.hashToken(rawToken);

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setTokenHash(hash);
        entity.setExpiresAt(OffsetDateTime.now().minusHours(1));
        entity.setRevokedAt(null);

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(entity));

        ApiException ex = assertThrows(ApiException.class,
                () -> refreshTokenService.validateAndGet(rawToken));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Refresh token has expired.", ex.getMessage());
    }

    @Test
    void shouldRevokeToken() {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setTokenHash("some-hash");
        entity.setExpiresAt(OffsetDateTime.now().plusDays(1));

        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        refreshTokenService.revoke(entity);

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertNotNull(captor.getValue().getRevokedAt());
    }

    @Test
    void shouldReturnCorrectExpirationSeconds() {
        assertEquals(7 * 24 * 60 * 60, refreshTokenService.getRefreshTokenExpirationSeconds());
    }

    @Test
    void shouldFailFastWhenRefreshTokenExpirationIsNotPositive() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> new RefreshTokenServiceImpl(refreshTokenRepository, 0));

        assertEquals(
                "barter.jwt.refresh-token-expiration-days (JWT_REFRESH_EXPIRATION_DAYS) must be greater than 0.",
                ex.getMessage());
    }
}

