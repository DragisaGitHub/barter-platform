package com.barterplatform.application.identity.service.impl;

import com.barterplatform.api.model.*;
import com.barterplatform.application.identity.auth.JwtService;
import com.barterplatform.application.identity.auth.RefreshTokenService;
import com.barterplatform.application.identity.mapper.RoleMapper;
import com.barterplatform.application.identity.mapper.UserMapper;
import com.barterplatform.application.identity.service.AuthService;
import com.barterplatform.application.identity.service.EmailVerificationService;
import com.barterplatform.application.identity.service.MailSender;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.*;
import com.barterplatform.domain.identity.enums.PreferredLanguage;
import com.barterplatform.domain.identity.enums.RoleCode;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.infrastructure.identity.repository.PasswordResetTokenRepository;
import com.barterplatform.infrastructure.identity.repository.RoleRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long PASSWORD_RESET_EXPIRATION_MINUTES = 30;

    @Value("${barter.email-verification.enabled:true}")
    private boolean emailVerificationEnabled = true;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailSender mailSender;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;

    @Override
    public CurrentUserResponse registerUser(RegisterUserRequest request) {
        validateEmailIsAvailable(request.getEmail());
        validateUsernameIsAvailable(request.getUsername());

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(emailVerificationEnabled ? UserStatus.PENDING_VERIFICATION : UserStatus.ACTIVE);
        user.setEmailVerified(!emailVerificationEnabled);
        user.setMfaEnabled(false);
        user.setPreferredLanguage(PreferredLanguage.SR);

        UserEntity savedUser = userRepository.save(user);
        RoleEntity userRole = resolveUserRole();
        userRoleRepository.save(createUserRole(savedUser.getId(), userRole.getId()));

        if (emailVerificationEnabled) {
            emailVerificationService.createAndSendVerificationCode(savedUser.getId(), savedUser.getEmail());
        }

        CurrentUserResponse response = userMapper.toCurrentUserResponse(savedUser);
        response.setRoles(List.of(roleMapper.toResponse(userRole)));
        response.setPermissions(List.of());
        response.setOauthAccounts(List.of());
        response.setMfaSettings(null);
        return response;
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        UserEntity user = findUserByIdentifier(request.getIdentifier());
        validatePassword(request.getPassword(), user.getPasswordHash());
        validateUserStatus(user);

        List<String> roles = resolveRoleNames(user.getId());

        String accessToken = jwtService.generateAccessToken(user.getUuid(), user.getUsername(), roles);
        RefreshTokenService.RefreshTokenResult refreshResult = refreshTokenService.createRefreshToken(user.getId());

        ensurePreferredLanguage(user);
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        CurrentUserResponse userResponse = buildCurrentUserResponse(user);

        return new TokenResponse()
                .accessToken(accessToken)
                .refreshToken(refreshResult.rawToken())
                .tokenType(TokenResponse.TokenTypeEnum.BEARER)
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .refreshExpiresIn(refreshTokenService.getRefreshTokenExpirationSeconds())
                .user(userResponse);
    }

    @Override
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        Optional<UserEntity> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            return new MessageResponse().message("If an account exists for this email, a password reset link has been sent.");
        }

        UserEntity user = userOptional.get();

        String rawToken = generateResetToken();
        String tokenHash = hashToken(rawToken);

        PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
        entity.setUserId(user.getId());
        entity.setTokenHash(tokenHash);
        entity.setExpiresAt(OffsetDateTime.now().plusMinutes(PASSWORD_RESET_EXPIRATION_MINUTES));

        passwordResetTokenRepository.save(entity);

        mailSender.sendHtml(
                user.getEmail(),
                "Barter Platform – Reset your password",
                buildPasswordResetEmailHtml(user.getEmail(), rawToken)
        );

        return new MessageResponse().message("If an account exists for this email, a password reset link has been sent.");
    }

    @Override
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "User not found."));

        PasswordResetTokenEntity tokenEntity = passwordResetTokenRepository
                .findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.BAD_REQUEST,
                        "Invalid or expired password reset token."));

        if (tokenEntity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Invalid or expired password reset token.");
        }

        String providedHash = hashToken(request.getToken());

        if (!providedHash.equals(tokenEntity.getTokenHash())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Invalid or expired password reset token.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        ensurePreferredLanguage(user);
        userRepository.save(user);

        tokenEntity.setUsedAt(OffsetDateTime.now());
        passwordResetTokenRepository.save(tokenEntity);

        return new MessageResponse().message("Password reset successfully.");
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String rawRefreshToken = requireRefreshToken(request);
        RefreshTokenEntity tokenEntity = refreshTokenService.validateAndGet(rawRefreshToken);

        UserEntity user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        ErrorCode.UNAUTHORIZED,
                        "User not found."));

        validateUserStatusForRefresh(user, tokenEntity);
        ensurePreferredLanguage(user);

        List<String> roles = resolveRoleNames(user.getId());
        String accessToken = jwtService.generateAccessToken(user.getUuid(), user.getUsername(), roles);

        // Rotate refresh token
        refreshTokenService.revoke(tokenEntity);
        RefreshTokenService.RefreshTokenResult newRefreshResult = refreshTokenService.createRefreshToken(user.getId());

        CurrentUserResponse userResponse = buildCurrentUserResponse(user);

        return new TokenResponse()
                .accessToken(accessToken)
                .refreshToken(newRefreshResult.rawToken())
                .tokenType(TokenResponse.TokenTypeEnum.BEARER)
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .refreshExpiresIn(refreshTokenService.getRefreshTokenExpirationSeconds())
                .user(userResponse);
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        String rawRefreshToken = requireRefreshToken(request);
        RefreshTokenEntity tokenEntity = refreshTokenService.validateAndGet(rawRefreshToken);
        refreshTokenService.revoke(tokenEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(UUID userUuid) {
        UserEntity user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "User not found."));

        ensurePreferredLanguage(user);
        return buildCurrentUserResponse(user);
    }

    @Override
    public MessageResponse verifyEmail(VerifyEmailRequest request) {
        return emailVerificationService.verifyEmail(request);
    }

    @Override
    public MessageResponse resendVerificationCode(ResendVerificationCodeRequest request) {
        return emailVerificationService.resendVerificationCode(request);
    }

    private UserEntity findUserByIdentifier(String identifier) {
        Optional<UserEntity> user = identifier.contains("@")
                ? userRepository.findByEmailIgnoreCase(identifier)
                : userRepository.findByUsernameIgnoreCase(identifier);

        return user.orElseThrow(() -> new ApiException(
                HttpStatus.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED,
                "Invalid credentials."));
    }

    private void validatePassword(String rawPassword, String passwordHash) {
        if (!passwordEncoder.matches(rawPassword, passwordHash)) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCode.UNAUTHORIZED,
                    "Invalid credentials.");
        }
    }

    private void validateUserStatus(UserEntity user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.FORBIDDEN,
                    "Account is suspended.");
        }
        if (user.getStatus() == UserStatus.BANNED) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.FORBIDDEN,
                    "Account is banned.");
        }
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION || !user.isEmailVerified()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.FORBIDDEN,
                    "Email verification required.");
        }
    }

    private void validateUserStatusForRefresh(UserEntity user, RefreshTokenEntity tokenEntity) {
        try {
            validateUserStatus(user);
        } catch (ApiException ex) {
            if (ex.getStatus() == HttpStatus.FORBIDDEN && tokenEntity.getRevokedAt() == null) {
                refreshTokenService.revokePresentedTokenForRejectedRefresh(tokenEntity);
            }
            throw ex;
        }
    }

    private String requireRefreshToken(RefreshTokenRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Refresh token is required.");
        }
        return request.getRefreshToken().trim();
    }

    private List<String> resolveRoleNames(Long userId) {
        List<UserRoleEntity> userRoles = userRoleRepository.findAllByIdUserIdOrderByAssignedAtAsc(userId);
        return userRoles.stream()
                .map(ur -> roleRepository.findById(ur.getId().getRoleId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(role -> role.getCode().name())
                .toList();
    }

    private CurrentUserResponse buildCurrentUserResponse(UserEntity user) {
        ensurePreferredLanguage(user);
        List<UserRoleEntity> userRoles = userRoleRepository.findAllByIdUserIdOrderByAssignedAtAsc(user.getId());
        List<RoleEntity> roles = userRoles.stream()
                .map(ur -> roleRepository.findById(ur.getId().getRoleId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        CurrentUserResponse response = userMapper.toCurrentUserResponse(user);
        response.setRoles(roleMapper.toResponseList(roles));
        response.setPermissions(List.of());
        response.setOauthAccounts(List.of());
        response.setMfaSettings(null);
        return response;
    }

    private void ensurePreferredLanguage(UserEntity user) {
        if (user.getPreferredLanguage() == null) {
            user.setPreferredLanguage(PreferredLanguage.SR);
        }
    }

    private void validateEmailIsAvailable(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    "Email '%s' is already in use.".formatted(email));
        }
    }

    private void validateUsernameIsAvailable(String username) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    "Username '%s' is already in use.".formatted(username));
        }
    }

    private RoleEntity resolveUserRole() {
        return roleRepository.findByCode(RoleCode.USER)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Role with code '%s' was not found.".formatted(RoleCode.USER)));
    }

    private UserRoleEntity createUserRole(Long userId, Long roleId) {
        UserRoleId userRoleId = new UserRoleId();
        userRoleId.setUserId(userId);
        userRoleId.setRoleId(roleId);

        UserRoleEntity userRoleEntity = new UserRoleEntity();
        userRoleEntity.setId(userRoleId);
        userRoleEntity.setAssignedAt(OffsetDateTime.now());
        return userRoleEntity;
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String buildPasswordResetEmailHtml(String email, String token) {
        String resetUrl = "http://localhost:5173/reset-password?email=%s&token=%s".formatted(email, token);

        return """
            <!DOCTYPE html>
            <html lang="en">
            <body style="margin:0;padding:0;background:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" role="presentation"
                     style="background:#f1f5f9;padding:40px 16px;">
                <tr>
                  <td align="center">
                    <table width="100%%" cellpadding="0" cellspacing="0" role="presentation"
                           style="max-width:480px;background:#ffffff;border-radius:12px;box-shadow:0 1px 3px rgba(0,0,0,.08);">
                      <tr>
                        <td style="padding:28px 40px 20px;text-align:center;border-bottom:1px solid #e2e8f0;">
                          <span style="font-size:22px;font-weight:700;color:#4f46e5;letter-spacing:-0.5px;">Barter Platform</span>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:32px 40px;">
                          <h1 style="margin:0 0 8px;font-size:20px;font-weight:700;color:#0f172a;">
                            Reset your password
                          </h1>
                          <p style="margin:0 0 24px;font-size:15px;color:#475569;line-height:1.6;">
                            We received a password reset request for <strong>%s</strong>.
                            This link expires in <strong>%d minutes</strong>.
                          </p>
                          <p style="margin:0 0 24px;">
                            <a href="%s"
                               style="display:inline-block;background:#4f46e5;color:#ffffff;text-decoration:none;
                                      padding:12px 20px;border-radius:8px;font-weight:600;font-size:14px;">
                              Reset password
                            </a>
                          </p>
                          <p style="margin:0;font-size:13px;color:#94a3b8;line-height:1.6;">
                            If you did not request this, you can safely ignore this email.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(email, AuthServiceImpl.PASSWORD_RESET_EXPIRATION_MINUTES, resetUrl);
    }
}
