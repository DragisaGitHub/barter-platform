package com.barterplatform.application.identity.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.ResendVerificationCodeRequest;
import com.barterplatform.api.model.VerifyEmailRequest;
import com.barterplatform.application.identity.auth.EmailVerificationResult;
import com.barterplatform.application.identity.auth.VerificationCodeHasher;
import com.barterplatform.application.identity.service.MailSender;
import com.barterplatform.common.exception.RateLimitException;
import com.barterplatform.domain.identity.entity.EmailVerificationCodeEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationCodeRepository codeRepository;
    @Mock private MailSender mailSender;
    @Mock private VerificationCodeHasher codeHasher;

    private EmailVerificationServiceImpl service;
    private UserEntity user;
    private EmailVerificationCodeEntity code;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationServiceImpl(userRepository, codeRepository, mailSender, codeHasher,
                15, 5, java.time.Duration.ofSeconds(60), 5, java.time.Duration.ofHours(1));
        user = new UserEntity();
        user.setId(10L);
        user.setEmail("person@example.com");
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        code = new EmailVerificationCodeEntity();
        code.setId(20L);
        code.setUserId(user.getId());
        code.setCodeHash("expected-hash");
        code.setCreatedAt(OffsetDateTime.now().minusMinutes(2));
        code.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
    }

    @Test
    void wrongCodeIncrementsAndPersistsAttempt() {
        prepareLatestCode();
        when(codeHasher.matches(user.getId(), "000000", code.getCodeHash())).thenReturn(false);

        assertEquals(EmailVerificationResult.INVALID, service.verifyEmail(new VerifyEmailRequest("person@example.com", "000000")));

        assertEquals(1, code.getFailedAttempts());
        verify(codeRepository).save(code);
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void fifthWrongCodeInvalidatesCodeAndReturnsLocked() {
        code.setFailedAttempts(4);
        prepareLatestCode();
        when(codeHasher.matches(user.getId(), "000000", code.getCodeHash())).thenReturn(false);

        assertEquals(EmailVerificationResult.LOCKED, service.verifyEmail(new VerifyEmailRequest("person@example.com", "000000")));

        assertEquals(5, code.getFailedAttempts());
        assertNotNull(code.getInvalidatedAt());
        verify(codeRepository).save(code);
    }

    @Test
    void lockedCodeCannotBeConsumed() {
        code.setFailedAttempts(5);
        code.setInvalidatedAt(OffsetDateTime.now());
        prepareLatestCode();

        assertEquals(EmailVerificationResult.LOCKED, service.verifyEmail(new VerifyEmailRequest("person@example.com", "123456")));
        verify(codeHasher, never()).matches(anyLong(), anyString(), anyString());
        verify(codeRepository, never()).save(any(EmailVerificationCodeEntity.class));
    }

    @Test
    void expiredCodeDoesNotConsumeAttempt() {
        code.setExpiresAt(OffsetDateTime.now().minusSeconds(1));
        prepareLatestCode();

        assertEquals(EmailVerificationResult.EXPIRED, service.verifyEmail(new VerifyEmailRequest("person@example.com", "123456")));
        assertEquals(0, code.getFailedAttempts());
        verify(codeRepository, never()).save(any(EmailVerificationCodeEntity.class));
    }

    @Test
    void correctCodeMarksCodeUsedAndActivatesUser() {
        prepareLatestCode();
        when(codeHasher.matches(user.getId(), "123456", code.getCodeHash())).thenReturn(true);

        assertEquals(EmailVerificationResult.VERIFIED, service.verifyEmail(new VerifyEmailRequest("person@example.com", "123456")));

        assertNotNull(code.getUsedAt());
        assertTrue(user.isEmailVerified());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(codeRepository).save(code);
        verify(userRepository).save(user);
    }

    @Test
    void resendCooldownIsAppliedToAccountAndReturnsRetryAfter() {
        when(userRepository.findByEmailIgnoreCaseForUpdate("person@example.com")).thenReturn(Optional.of(user));
        code.setCreatedAt(OffsetDateTime.now());
        when(codeRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.of(code));

        RateLimitException exception = assertThrows(RateLimitException.class,
                () -> service.resendVerificationCode(new ResendVerificationCodeRequest("person@example.com")));

        assertEquals(429, exception.getStatus().value());
        assertTrue(exception.getRetryAfterSeconds() > 0);
        verify(codeRepository, never()).invalidateActiveCodes(anyLong(), any());
        verify(mailSender, never()).sendHtml(anyString(), anyString(), anyString());
    }

    @Test
    void accountIssuanceCapReturnsRetryAfter() {
        when(userRepository.findByEmailIgnoreCaseForUpdate("person@example.com")).thenReturn(Optional.of(user));
        when(codeRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.of(code));
        when(codeRepository.countByUserIdAndCreatedAtAfterAndCountsTowardIssuanceLimitTrue(anyLong(), any()))
                .thenReturn(5L);
        when(codeRepository.findFirstByUserIdAndCreatedAtAfterAndCountsTowardIssuanceLimitTrueOrderByCreatedAtAsc(
                anyLong(), any()))
                .thenReturn(Optional.of(code));

        RateLimitException exception = assertThrows(RateLimitException.class,
                () -> service.resendVerificationCode(new ResendVerificationCodeRequest("person@example.com")));

        assertEquals(429, exception.getStatus().value());
        assertTrue(exception.getRetryAfterSeconds() > 0);
        verify(codeRepository, never()).invalidateActiveCodes(anyLong(), any());
    }

    private void prepareLatestCode() {
        when(userRepository.findByEmailIgnoreCaseForUpdate("person@example.com")).thenReturn(Optional.of(user));
        when(codeRepository.findByUserIdForUpdate(eq(user.getId()), any(Pageable.class))).thenReturn(List.of(code));
    }
}
