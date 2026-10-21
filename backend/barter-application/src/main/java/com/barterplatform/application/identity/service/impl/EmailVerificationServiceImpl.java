package com.barterplatform.application.identity.service.impl;

import com.barterplatform.api.model.ResendVerificationCodeRequest;
import com.barterplatform.api.model.VerifyEmailRequest;
import com.barterplatform.application.identity.auth.EmailVerificationResult;
import com.barterplatform.application.identity.auth.VerificationCodeHasher;
import com.barterplatform.application.identity.service.EmailVerificationService;
import com.barterplatform.application.identity.service.MailSender;
import com.barterplatform.api.model.MessageResponse;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.common.exception.RateLimitException;
import com.barterplatform.domain.identity.entity.EmailVerificationCodeEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationServiceImpl.class);

    private final UserRepository userRepository;
    private final EmailVerificationCodeRepository verificationCodeRepository;
    private final MailSender mailSender;
    private final VerificationCodeHasher codeHasher;
    private final long codeExpirationMinutes;
    private final int maxFailedAttempts;
    private final Duration resendCooldown;
    private final int maxCodesPerWindow;
    private final Duration issuanceWindow;

    public EmailVerificationServiceImpl(
            UserRepository userRepository,
            EmailVerificationCodeRepository verificationCodeRepository,
            MailSender mailSender,
            VerificationCodeHasher codeHasher,
            @Value("${barter.verification.code-expiration-minutes:15}") long codeExpirationMinutes,
            @Value("${barter.verification.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${barter.verification.resend-cooldown:60s}") Duration resendCooldown,
            @Value("${barter.verification.max-codes-per-window:5}") int maxCodesPerWindow,
            @Value("${barter.verification.issuance-window:1h}") Duration issuanceWindow) {
        this.userRepository = userRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.mailSender = mailSender;
        this.codeHasher = codeHasher;
        this.codeExpirationMinutes = codeExpirationMinutes;
        this.maxFailedAttempts = Math.max(1, maxFailedAttempts);
        this.resendCooldown = positiveOrDefault(resendCooldown, Duration.ofSeconds(60));
        this.maxCodesPerWindow = Math.max(1, maxCodesPerWindow);
        this.issuanceWindow = positiveOrDefault(issuanceWindow, Duration.ofHours(1));
    }

    @Override
    public void createAndSendVerificationCode(Long userId, String email) {
        String rawCode = generateCode();
        OffsetDateTime now = OffsetDateTime.now();

        EmailVerificationCodeEntity entity = new EmailVerificationCodeEntity();
        entity.setUserId(userId);
        entity.setCodeHash(codeHasher.hash(userId, rawCode));
        entity.setExpiresAt(now.plusMinutes(codeExpirationMinutes));
        entity.setCreatedAt(now);
        entity.setCountsTowardIssuanceLimit(true);
        verificationCodeRepository.save(entity);

        sendAfterCommit(email, rawCode);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailVerificationResult verifyEmail(VerifyEmailRequest request) {
        Optional<UserEntity> userOptional = userRepository.findByEmailIgnoreCaseForUpdate(request.getEmail());
        if (userOptional.isEmpty()) {
            return EmailVerificationResult.MISSING;
        }
        UserEntity user = userOptional.get();
        validateAccountStatusForVerification(user);

        if (user.isEmailVerified()) {
            return EmailVerificationResult.ALREADY_VERIFIED;
        }

        EmailVerificationCodeEntity codeEntity = verificationCodeRepository
                .findByUserIdForUpdate(user.getId(), PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
        if (codeEntity == null || codeEntity.getUsedAt() != null) {
            return EmailVerificationResult.MISSING;
        }
        if (codeEntity.getInvalidatedAt() != null) {
            return codeEntity.getFailedAttempts() >= maxFailedAttempts
                    ? EmailVerificationResult.LOCKED
                    : EmailVerificationResult.MISSING;
        }

        if (codeEntity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            return EmailVerificationResult.EXPIRED;
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (!codeHasher.matches(user.getId(), request.getCode(), codeEntity.getCodeHash())) {
            int attempts = codeEntity.getFailedAttempts() + 1;
            codeEntity.setFailedAttempts(attempts);
            if (attempts >= maxFailedAttempts) {
                codeEntity.setInvalidatedAt(now);
                verificationCodeRepository.save(codeEntity);
                return EmailVerificationResult.LOCKED;
            }
            verificationCodeRepository.save(codeEntity);
            return EmailVerificationResult.INVALID;
        }

        codeEntity.setUsedAt(now);
        verificationCodeRepository.save(codeEntity);

        user.setEmailVerified(true);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);

        return EmailVerificationResult.VERIFIED;
    }

    private void validateAccountStatusForVerification(UserEntity user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Account is suspended.");
        }
        if (user.getStatus() == UserStatus.BANNED) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Account is banned.");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Account is deleted.");
        }
    }

    @Override
    public MessageResponse resendVerificationCode(ResendVerificationCodeRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCaseForUpdate(request.getEmail())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "User not found."));

        if (user.isEmailVerified()) {
            return new MessageResponse().message("Email is already verified.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        Optional<EmailVerificationCodeEntity> latest = verificationCodeRepository
                .findFirstByUserIdOrderByCreatedAtDesc(user.getId());
        if (latest.isPresent()) {
            EmailVerificationCodeEntity latestCode = latest.get();
            boolean lockedByAttempts = latestCode.getInvalidatedAt() != null
                    && latestCode.getFailedAttempts() >= maxFailedAttempts;
            OffsetDateTime cooldownEndsAt = latestCode.getCreatedAt().plus(resendCooldown);
            if (!lockedByAttempts && now.isBefore(cooldownEndsAt)) {
                throw new RateLimitException("Please wait before requesting another verification code.",
                        retryAfterSeconds(now, cooldownEndsAt));
            }
        }

        OffsetDateTime windowStart = now.minus(issuanceWindow);
        long codeCount = verificationCodeRepository
                .countByUserIdAndCreatedAtAfterAndCountsTowardIssuanceLimitTrue(user.getId(), windowStart);
        if (codeCount >= maxCodesPerWindow) {
            OffsetDateTime nextAvailableAt = verificationCodeRepository
                    .findFirstByUserIdAndCreatedAtAfterAndCountsTowardIssuanceLimitTrueOrderByCreatedAtAsc(
                            user.getId(), windowStart)
                    .map(code -> code.getCreatedAt().plus(issuanceWindow))
                    .orElse(now.plusSeconds(1));
            throw new RateLimitException("Too many verification codes have been requested. Please retry later.",
                    retryAfterSeconds(now, nextAvailableAt));
        }

        verificationCodeRepository.invalidateActiveCodes(user.getId(), now);
        createAndSendVerificationCode(user.getId(), user.getEmail());
        return new MessageResponse().message("Verification code sent.");
    }

    private String generateCode() {
        int code = SECURE_RANDOM.nextInt(900_000) + 100_000; // 100000-999999
        return String.valueOf(code);
    }

    private static Duration positiveOrDefault(Duration value, Duration fallback) {
        return value != null && !value.isZero() && !value.isNegative() ? value : fallback;
    }

    private long retryAfterSeconds(OffsetDateTime now, OffsetDateTime availableAt) {
        long millis = Duration.between(now, availableAt).toMillis();
        return Math.max(1, (long) Math.ceil(millis / 1000.0d));
    }

    private void sendAfterCommit(String email, String rawCode) {
        Runnable send = () -> {
            try {
                mailSender.sendHtml(
                        email,
                        "Barter Platform – Verify your email",
                        buildVerificationEmailHtml(rawCode, codeExpirationMinutes));
            } catch (RuntimeException e) {
                log.warn("Verification email delivery failed after commit; a resend may be requested.");
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private static String buildVerificationEmailHtml(String code, long expirationMinutes) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
                  <title>Verify your email</title>
                </head>
                <body style="margin:0;padding:0;background:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" role="presentation"
                         style="background:#f1f5f9;padding:40px 16px;">
                    <tr>
                      <td align="center">
                        <table width="100%%" cellpadding="0" cellspacing="0" role="presentation"
                               style="max-width:480px;background:#ffffff;border-radius:12px;box-shadow:0 1px 3px rgba(0,0,0,.08);">
                          <!-- Header -->
                          <tr>
                            <td style="padding:28px 40px 20px;text-align:center;border-bottom:1px solid #e2e8f0;">
                              <span style="font-size:22px;font-weight:700;color:#4f46e5;letter-spacing:-0.5px;">Barter Platform</span>
                            </td>
                          </tr>
                          <!-- Body -->
                          <tr>
                            <td style="padding:32px 40px;">
                              <h1 style="margin:0 0 8px;font-size:20px;font-weight:700;color:#0f172a;">
                                Verify your email address
                              </h1>
                              <p style="margin:0 0 24px;font-size:15px;color:#475569;line-height:1.6;">
                                Thanks for signing up! Use the code below to verify your email address.
                                It expires in <strong>%d minutes</strong>.
                              </p>
                              <!-- Code box -->
                              <div style="background:#f8fafc;border:2px solid #e0e7ff;border-radius:10px;
                                          padding:22px;text-align:center;margin-bottom:24px;">
                                <span style="font-size:38px;font-weight:700;letter-spacing:12px;
                                             color:#4f46e5;font-family:'Courier New',monospace;">%s</span>
                              </div>
                              <p style="margin:0;font-size:13px;color:#94a3b8;line-height:1.6;">
                                If you didn't create an account on Barter Platform, you can safely ignore this email.
                              </p>
                            </td>
                          </tr>
                          <!-- Footer -->
                          <tr>
                            <td style="padding:16px 40px 24px;border-top:1px solid #e2e8f0;text-align:center;">
                              <p style="margin:0;font-size:12px;color:#94a3b8;">
                                &copy; 2026 Barter Platform &middot; You're receiving this because you created an account.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(expirationMinutes, code);
    }
}

