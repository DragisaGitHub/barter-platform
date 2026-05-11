package com.barterplatform.application.identity.service.impl;

import com.barterplatform.api.model.MessageResponse;
import com.barterplatform.api.model.ResendVerificationCodeRequest;
import com.barterplatform.api.model.VerifyEmailRequest;
import com.barterplatform.application.identity.service.EmailVerificationService;
import com.barterplatform.application.identity.service.MailSender;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.EmailVerificationCodeEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailVerificationCodeRepository verificationCodeRepository;
    private final MailSender mailSender;
    private final long codeExpirationMinutes;

    public EmailVerificationServiceImpl(
            UserRepository userRepository,
            EmailVerificationCodeRepository verificationCodeRepository,
            MailSender mailSender,
            @Value("${barter.verification.code-expiration-minutes:15}") long codeExpirationMinutes) {
        this.userRepository = userRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.mailSender = mailSender;
        this.codeExpirationMinutes = codeExpirationMinutes;
    }

    @Override
    public void createAndSendVerificationCode(Long userId, String email) {
        String rawCode = generateCode();
        String codeHash = hashCode(rawCode);

        EmailVerificationCodeEntity entity = new EmailVerificationCodeEntity();
        entity.setUserId(userId);
        entity.setCodeHash(codeHash);
        entity.setExpiresAt(OffsetDateTime.now().plusMinutes(codeExpirationMinutes));
        entity.setCreatedAt(OffsetDateTime.now());
        verificationCodeRepository.save(entity);

        mailSender.sendHtml(
                email,
                "Barter Platform – Verify your email",
                buildVerificationEmailHtml(rawCode, codeExpirationMinutes)
        );
    }

    @Override
    public MessageResponse verifyEmail(VerifyEmailRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "User not found."));

        if (user.isEmailVerified()) {
            return new MessageResponse().message("Email is already verified.");
        }

        EmailVerificationCodeEntity codeEntity = verificationCodeRepository
                .findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.BAD_REQUEST,
                        "No verification code found. Please request a new one."));

        if (codeEntity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Verification code has expired. Please request a new one.");
        }

        String providedHash = hashCode(request.getCode());
        if (!providedHash.equals(codeEntity.getCodeHash())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Invalid verification code.");
        }

        // Mark code as used
        codeEntity.setUsedAt(OffsetDateTime.now());
        verificationCodeRepository.save(codeEntity);

        // Activate user
        user.setEmailVerified(true);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);

        return new MessageResponse().message("Email verified successfully.");
    }

    @Override
    public MessageResponse resendVerificationCode(ResendVerificationCodeRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "User not found."));

        if (user.isEmailVerified()) {
            return new MessageResponse().message("Email is already verified.");
        }

        createAndSendVerificationCode(user.getId(), user.getEmail());
        return new MessageResponse().message("Verification code sent.");
    }

    private String generateCode() {
        int code = SECURE_RANDOM.nextInt(900_000) + 100_000; // 100000-999999
        return String.valueOf(code);
    }

    public static String hashCode(String rawCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawCode.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
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

