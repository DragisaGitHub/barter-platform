package com.barterplatform.application.identity.service.impl;

import com.barterplatform.api.model.CurrentUserResponse;
import com.barterplatform.api.model.LoginRequest;
import com.barterplatform.api.model.MessageResponse;
import com.barterplatform.api.model.RefreshTokenRequest;
import com.barterplatform.api.model.RegisterUserRequest;
import com.barterplatform.api.model.ResendVerificationCodeRequest;
import com.barterplatform.api.model.TokenResponse;
import com.barterplatform.api.model.VerifyEmailRequest;
import com.barterplatform.application.identity.auth.JwtService;
import com.barterplatform.application.identity.auth.RefreshTokenService;
import com.barterplatform.application.identity.mapper.RoleMapper;
import com.barterplatform.application.identity.mapper.UserMapper;
import com.barterplatform.application.identity.service.AuthService;
import com.barterplatform.application.identity.service.EmailVerificationService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.RefreshTokenEntity;
import com.barterplatform.domain.identity.entity.RoleEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.entity.UserRoleEntity;
import com.barterplatform.domain.identity.entity.UserRoleId;
import com.barterplatform.domain.identity.enums.RoleCode;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.infrastructure.identity.repository.RoleRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

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
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);
        user.setMfaEnabled(false);

        UserEntity savedUser = userRepository.save(user);
        RoleEntity userRole = resolveUserRole();
        userRoleRepository.save(createUserRole(savedUser.getId(), userRole.getId()));

        // Generate and send email verification code
        emailVerificationService.createAndSendVerificationCode(savedUser.getId(), savedUser.getEmail());

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
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        RefreshTokenEntity tokenEntity = refreshTokenService.validateAndGet(request.getRefreshToken());

        UserEntity user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        ErrorCode.UNAUTHORIZED,
                        "User not found."));

        validateUserStatus(user);

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
        RefreshTokenEntity tokenEntity = refreshTokenService.validateAndGet(request.getRefreshToken());
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
                ? userRepository.findByEmail(identifier)
                : userRepository.findByUsername(identifier);

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

    private void validateEmailIsAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    "Email '%s' is already in use.".formatted(email));
        }
    }

    private void validateUsernameIsAvailable(String username) {
        if (userRepository.existsByUsername(username)) {
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
}
