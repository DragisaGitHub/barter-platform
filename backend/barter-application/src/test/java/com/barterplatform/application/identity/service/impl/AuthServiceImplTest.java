package com.barterplatform.application.identity.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.*;
import com.barterplatform.application.identity.auth.JwtService;
import com.barterplatform.application.identity.auth.RefreshTokenService;
import com.barterplatform.application.identity.mapper.RoleMapper;
import com.barterplatform.application.identity.mapper.UserMapper;
import com.barterplatform.application.identity.service.EmailVerificationService;
import com.barterplatform.application.identity.service.MailSender;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.RefreshTokenEntity;
import com.barterplatform.domain.identity.entity.RoleEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.entity.UserRoleEntity;
import com.barterplatform.domain.identity.entity.UserRoleId;
import com.barterplatform.domain.identity.enums.PreferredLanguage;
import com.barterplatform.domain.identity.enums.RoleCode;
import com.barterplatform.infrastructure.identity.repository.RoleRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import java.time.OffsetDateTime;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private com.barterplatform.infrastructure.identity.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private MailSender mailSender;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void defaultEmailVerificationEnabled() {
        ReflectionTestUtils.setField(authService, "emailVerificationEnabled", true);
    }

    @Test
    void shouldRegisterUserWithHashedPasswordAndAssignedUserRole() {
        RegisterUserRequest request = new RegisterUserRequest("alex99", "alex@example.com", "P@ssword123");
        RoleEntity userRole = userRoleEntity();
        RoleResponse userRoleResponse = new RoleResponse()
                .uuid(userRole.getUuid())
                .code(com.barterplatform.api.model.RoleCode.USER)
                .name(userRole.getName())
                .description(userRole.getDescription())
                .createdAt(userRole.getCreatedAt());

        when(userRepository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase(request.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(101L);
            user.setUuid(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            user.setCreatedAt(OffsetDateTime.parse("2026-05-08T10:15:30Z"));
            return user;
        });
        when(roleRepository.findByCode(RoleCode.USER)).thenReturn(Optional.of(userRole));
        when(userRoleRepository.save(any(UserRoleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toCurrentUserResponse(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            return new CurrentUserResponse()
                    .uuid(user.getUuid())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .status(UserStatus.PENDING_VERIFICATION)
                    .emailVerified(false)
                    .mfaEnabled(false)
                    .preferredLanguage(com.barterplatform.api.model.PreferredLanguage.SR)
                    .createdAt(user.getCreatedAt());
        });
        when(roleMapper.toResponse(userRole)).thenReturn(userRoleResponse);

        CurrentUserResponse response = authService.registerUser(request);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();
        assertEquals("alex99", savedUser.getUsername());
        assertEquals("alex@example.com", savedUser.getEmail());
        assertEquals("hashed-password", savedUser.getPasswordHash());
        assertEquals(com.barterplatform.domain.identity.enums.UserStatus.PENDING_VERIFICATION, savedUser.getStatus());
        assertEquals(PreferredLanguage.SR, savedUser.getPreferredLanguage());
        assertFalse(savedUser.isEmailVerified());
        assertFalse(savedUser.isMfaEnabled());

        ArgumentCaptor<UserRoleEntity> userRoleCaptor = ArgumentCaptor.forClass(UserRoleEntity.class);
        verify(userRoleRepository).save(userRoleCaptor.capture());
        UserRoleEntity savedUserRole = userRoleCaptor.getValue();
        assertEquals(101L, savedUserRole.getId().getUserId());
        assertEquals(userRole.getId(), savedUserRole.getId().getRoleId());
        assertNotNull(savedUserRole.getAssignedAt());
        assertNull(savedUserRole.getAssignedBy());

        assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), response.getUuid());
        assertEquals("alex99", response.getUsername());
        assertEquals("alex@example.com", response.getEmail());
        assertEquals(UserStatus.PENDING_VERIFICATION, response.getStatus());
        assertEquals(com.barterplatform.api.model.PreferredLanguage.SR, response.getPreferredLanguage());
        assertFalse(response.getEmailVerified());
        assertFalse(response.getMfaEnabled());
        assertEquals(1, response.getRoles().size());
        assertSame(userRoleResponse, response.getRoles().getFirst());
        assertEquals(0, response.getPermissions().size());
        assertEquals(0, response.getOauthAccounts().size());
        assertNull(response.getMfaSettings());

        verify(emailVerificationService).createAndSendVerificationCode(101L, "alex@example.com");
        verify(roleRepository).findByCode(RoleCode.USER);
        verify(userMapper).toCurrentUserResponse(any(UserEntity.class));
        verify(roleMapper).toResponse(userRole);
    }

    @Test
    void shouldRegisterActiveVerifiedUserAndSkipVerificationEmailWhenEmailVerificationDisabled() {
        ReflectionTestUtils.setField(authService, "emailVerificationEnabled", false);

        RegisterUserRequest request = new RegisterUserRequest("devuser", "dev@example.com", "P@ssword123");
        RoleEntity userRole = userRoleEntity();
        RoleResponse userRoleResponse = new RoleResponse()
                .uuid(userRole.getUuid())
                .code(com.barterplatform.api.model.RoleCode.USER)
                .name(userRole.getName())
                .description(userRole.getDescription())
                .createdAt(userRole.getCreatedAt());

        when(userRepository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase(request.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(202L);
            user.setUuid(UUID.fromString("33333333-3333-3333-3333-333333333333"));
            user.setCreatedAt(OffsetDateTime.parse("2026-05-08T10:15:30Z"));
            return user;
        });
        when(roleRepository.findByCode(RoleCode.USER)).thenReturn(Optional.of(userRole));
        when(userRoleRepository.save(any(UserRoleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toCurrentUserResponse(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            return new CurrentUserResponse()
                    .uuid(user.getUuid())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .mfaEnabled(false)
                    .preferredLanguage(com.barterplatform.api.model.PreferredLanguage.SR)
                    .createdAt(user.getCreatedAt());
        });
        when(roleMapper.toResponse(userRole)).thenReturn(userRoleResponse);

        CurrentUserResponse response = authService.registerUser(request);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();
        assertEquals(com.barterplatform.domain.identity.enums.UserStatus.ACTIVE, savedUser.getStatus());
        assertTrue(savedUser.isEmailVerified());
        assertEquals(UserStatus.ACTIVE, response.getStatus());
        assertTrue(response.getEmailVerified());
        verify(emailVerificationService, never()).createAndSendVerificationCode(any(), any());
    }

    @Test
    void shouldThrowForbiddenForPendingVerificationUser() {
        LoginRequest request = new LoginRequest("alex@example.com", "P@ssword123");
        UserEntity user = activeUser();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);

        when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("P@ssword123", user.getPasswordHash())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        assertEquals("Email verification required.", ex.getMessage());
    }

    @Test
    void shouldThrowForbiddenForUnverifiedEmail() {
        LoginRequest request = new LoginRequest("alex@example.com", "P@ssword123");
        UserEntity user = activeUser();
        user.setEmailVerified(false);

        when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("P@ssword123", user.getPasswordHash())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        assertEquals("Email verification required.", ex.getMessage());
    }

    @Test
    void shouldThrowConflictWhenEmailAlreadyExists() {
        RegisterUserRequest request = new RegisterUserRequest("alex99", "alex@example.com", "P@ssword123");
        when(userRepository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> authService.registerUser(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(ErrorCode.CONFLICT, exception.getCode());
        assertEquals("Email 'alex@example.com' is already in use.", exception.getMessage());

        verify(userRepository, never()).existsByUsernameIgnoreCase(any());
        verifyNoInteractions(roleRepository, userRoleRepository, userMapper, roleMapper, passwordEncoder);
    }

    @Test
    void shouldThrowConflictWhenUsernameAlreadyExists() {
        RegisterUserRequest request = new RegisterUserRequest("alex99", "alex@example.com", "P@ssword123");
        when(userRepository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase(request.getUsername())).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> authService.registerUser(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(ErrorCode.CONFLICT, exception.getCode());
        assertEquals("Username 'alex99' is already in use.", exception.getMessage());

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(UserEntity.class));
        verifyNoInteractions(roleRepository, userRoleRepository, userMapper, roleMapper);
    }

    private RoleEntity userRoleEntity() {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(7L);
        roleEntity.setUuid(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        roleEntity.setCode(RoleCode.USER);
        roleEntity.setName("User");
        roleEntity.setDescription("Default user role");
        roleEntity.setCreatedAt(OffsetDateTime.parse("2026-05-08T10:15:30Z"));
        return roleEntity;
    }

    // --- Login tests ---

    @Test
    void shouldLoginSuccessfullyWithEmail() {
        LoginRequest request = new LoginRequest("alex@example.com", "P@ssword123");
        UserEntity user = activeUser();
        RoleEntity role = userRoleEntity();
        UserRoleEntity userRoleEntity = createTestUserRoleEntity(user.getId(), role.getId());

        when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("P@ssword123", user.getPasswordHash())).thenReturn(true);
        when(userRoleRepository.findAllByIdUserIdOrderByAssignedAtAsc(user.getId()))
                .thenReturn(List.of(userRoleEntity));
        when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
        when(jwtService.generateAccessToken(eq(user.getUuid()), eq(user.getUsername()), anyList()))
                .thenReturn("jwt-access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(1800L);
        when(refreshTokenService.createRefreshToken(user.getId()))
                .thenReturn(new RefreshTokenService.RefreshTokenResult("raw-refresh-token", new RefreshTokenEntity()));
        when(refreshTokenService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toCurrentUserResponse(any(UserEntity.class))).thenReturn(
                new CurrentUserResponse()
                        .uuid(user.getUuid())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .preferredLanguage(com.barterplatform.api.model.PreferredLanguage.EN));
        when(roleMapper.toResponseList(anyList())).thenReturn(List.of());

        TokenResponse response = authService.login(request);

        assertEquals("jwt-access-token", response.getAccessToken());
        assertEquals("raw-refresh-token", response.getRefreshToken());
        assertEquals(TokenResponse.TokenTypeEnum.BEARER, response.getTokenType());
        assertEquals(1800L, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals(com.barterplatform.api.model.PreferredLanguage.EN, response.getUser().getPreferredLanguage());

        verify(userRepository).findByEmailIgnoreCase("alex@example.com");
        verify(passwordEncoder).matches("P@ssword123", user.getPasswordHash());
    }

    @Test
    void shouldLoginSuccessfullyWithUsername() {
        LoginRequest request = new LoginRequest("alex99", "P@ssword123");
        UserEntity user = activeUser();
        RoleEntity role = userRoleEntity();
        UserRoleEntity userRoleEntity = createTestUserRoleEntity(user.getId(), role.getId());

        when(userRepository.findByUsernameIgnoreCase("alex99")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("P@ssword123", user.getPasswordHash())).thenReturn(true);
        when(userRoleRepository.findAllByIdUserIdOrderByAssignedAtAsc(user.getId()))
                .thenReturn(List.of(userRoleEntity));
        when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
        when(jwtService.generateAccessToken(eq(user.getUuid()), eq(user.getUsername()), anyList()))
                .thenReturn("jwt-access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(1800L);
        when(refreshTokenService.createRefreshToken(user.getId()))
                .thenReturn(new RefreshTokenService.RefreshTokenResult("raw-refresh-token", new RefreshTokenEntity()));
        when(refreshTokenService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toCurrentUserResponse(any(UserEntity.class))).thenReturn(
                new CurrentUserResponse().uuid(user.getUuid()).username(user.getUsername()));
        when(roleMapper.toResponseList(anyList())).thenReturn(List.of());

        TokenResponse response = authService.login(request);

        assertEquals("jwt-access-token", response.getAccessToken());
        verify(userRepository).findByUsernameIgnoreCase("alex99");
    }

    @Test
    void shouldThrowUnauthorizedForInvalidEmail() {
        LoginRequest request = new LoginRequest("unknown@example.com", "P@ssword123");
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
        assertEquals("Invalid credentials.", ex.getMessage());
    }

    @Test
    void shouldThrowUnauthorizedForWrongPassword() {
        LoginRequest request = new LoginRequest("alex@example.com", "wrong-password");
        UserEntity user = activeUser();

        when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
        assertEquals("Invalid credentials.", ex.getMessage());
    }

    @Test
    void shouldThrowForbiddenForSuspendedUser() {
        LoginRequest request = new LoginRequest("alex@example.com", "P@ssword123");
        UserEntity user = activeUser();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.SUSPENDED);

        when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("P@ssword123", user.getPasswordHash())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        assertEquals("Account is suspended.", ex.getMessage());
    }

    @Test
    void shouldThrowForbiddenForBannedUser() {
        LoginRequest request = new LoginRequest("alex@example.com", "P@ssword123");
        UserEntity user = activeUser();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.BANNED);

        when(userRepository.findByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("P@ssword123", user.getPasswordHash())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        assertEquals("Account is banned.", ex.getMessage());
    }

    // --- Refresh token tests ---

    @Test
    void shouldRefreshTokenSuccessfully() {
        RefreshTokenRequest request = new RefreshTokenRequest().refreshToken("raw-refresh-token");
        UserEntity user = activeUser();
        RoleEntity role = userRoleEntity();
        UserRoleEntity userRoleEntity = createTestUserRoleEntity(user.getId(), role.getId());

        RefreshTokenEntity existingToken = new RefreshTokenEntity();
        existingToken.setUserId(user.getId());

        when(refreshTokenService.validateAndGet("raw-refresh-token")).thenReturn(existingToken);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRoleRepository.findAllByIdUserIdOrderByAssignedAtAsc(user.getId()))
                .thenReturn(List.of(userRoleEntity));
        when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
        when(jwtService.generateAccessToken(eq(user.getUuid()), eq(user.getUsername()), anyList()))
                .thenReturn("new-jwt-access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(1800L);
        when(refreshTokenService.createRefreshToken(user.getId()))
                .thenReturn(new RefreshTokenService.RefreshTokenResult("new-raw-refresh-token", new RefreshTokenEntity()));
        when(refreshTokenService.getRefreshTokenExpirationSeconds()).thenReturn(604800L);
        when(userMapper.toCurrentUserResponse(any(UserEntity.class))).thenReturn(
                new CurrentUserResponse()
                        .uuid(user.getUuid())
                        .username(user.getUsername())
                        .preferredLanguage(com.barterplatform.api.model.PreferredLanguage.EN));
        when(roleMapper.toResponseList(anyList())).thenReturn(List.of());

        TokenResponse response = authService.refreshToken(request);

        assertEquals("new-jwt-access-token", response.getAccessToken());
        assertEquals("new-raw-refresh-token", response.getRefreshToken());
        assertEquals(TokenResponse.TokenTypeEnum.BEARER, response.getTokenType());
        assertEquals(1800L, response.getExpiresIn());
        assertEquals(604800L, response.getRefreshExpiresIn());
        assertNotNull(response.getUser());
        assertEquals(com.barterplatform.api.model.PreferredLanguage.EN, response.getUser().getPreferredLanguage());

        verify(refreshTokenService).revoke(existingToken);
        verify(refreshTokenService).createRefreshToken(user.getId());
    }

    @Test
    void shouldReturnCurrentUserWithPreferredLanguage() {
        UserEntity user = activeUser();
        RoleEntity role = userRoleEntity();
        UserRoleEntity userRoleEntity = createTestUserRoleEntity(user.getId(), role.getId());

        when(userRepository.findByUuid(user.getUuid())).thenReturn(Optional.of(user));
        when(userRoleRepository.findAllByIdUserIdOrderByAssignedAtAsc(user.getId()))
                .thenReturn(List.of(userRoleEntity));
        when(roleRepository.findById(role.getId())).thenReturn(Optional.of(role));
        when(userMapper.toCurrentUserResponse(any(UserEntity.class))).thenReturn(
                new CurrentUserResponse()
                        .uuid(user.getUuid())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .preferredLanguage(com.barterplatform.api.model.PreferredLanguage.EN));
        when(roleMapper.toResponseList(anyList())).thenReturn(List.of());

        CurrentUserResponse response = authService.getCurrentUser(user.getUuid());

        assertEquals(com.barterplatform.api.model.PreferredLanguage.EN, response.getPreferredLanguage());
        assertEquals(user.getUuid(), response.getUuid());
    }

    @Test
    void shouldThrowForbiddenOnRefreshWhenUserIsSuspended() {
        RefreshTokenRequest request = new RefreshTokenRequest().refreshToken("raw-refresh-token");
        UserEntity user = activeUser();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.SUSPENDED);

        RefreshTokenEntity existingToken = new RefreshTokenEntity();
        existingToken.setUserId(user.getId());

        when(refreshTokenService.validateAndGet("raw-refresh-token")).thenReturn(existingToken);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class, () -> authService.refreshToken(request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("Account is suspended.", ex.getMessage());
        verify(refreshTokenService).revokePresentedTokenForRejectedRefresh(existingToken);
        verify(refreshTokenService, never()).revoke(existingToken);
        verify(refreshTokenService, never()).createRefreshToken(user.getId());
    }

    @Test
    void shouldThrowForbiddenOnRefreshWhenUserIsBanned() {
        RefreshTokenRequest request = new RefreshTokenRequest().refreshToken("raw-refresh-token");
        UserEntity user = activeUser();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.BANNED);

        RefreshTokenEntity existingToken = new RefreshTokenEntity();
        existingToken.setUserId(user.getId());

        when(refreshTokenService.validateAndGet("raw-refresh-token")).thenReturn(existingToken);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class, () -> authService.refreshToken(request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("Account is banned.", ex.getMessage());
        verify(refreshTokenService).revokePresentedTokenForRejectedRefresh(existingToken);
        verify(refreshTokenService, never()).revoke(existingToken);
        verify(refreshTokenService, never()).createRefreshToken(user.getId());
    }

    @Test
    void shouldRejectBlankRefreshTokenOnRefresh() {
        RefreshTokenRequest request = new RefreshTokenRequest().refreshToken("   ");

        ApiException ex = assertThrows(ApiException.class, () -> authService.refreshToken(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        assertEquals("Refresh token is required.", ex.getMessage());
    }

    // --- Logout tests ---

    @Test
    void shouldLogoutSuccessfully() {
        RefreshTokenRequest request = new RefreshTokenRequest().refreshToken("raw-refresh-token");
        RefreshTokenEntity existingToken = new RefreshTokenEntity();

        when(refreshTokenService.validateAndGet("raw-refresh-token")).thenReturn(existingToken);

        authService.logout(request);

        verify(refreshTokenService).revoke(existingToken);
    }

    @Test
    void shouldRejectMissingRefreshTokenOnLogout() {
        ApiException ex = assertThrows(ApiException.class, () -> authService.logout(new RefreshTokenRequest()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
        assertEquals("Refresh token is required.", ex.getMessage());
    }

    @Test
    void forgotPassword_createsHashedTokenAndSendsEmail() throws Exception {
        UserEntity user = activeUser();

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = authService.forgotPassword(new ForgotPasswordRequest("alex@example.com"));

        assertEquals("If an account exists for this email, a password reset link has been sent.", response.getMessage());

        ArgumentCaptor<com.barterplatform.domain.identity.entity.PasswordResetTokenEntity> tokenCaptor = ArgumentCaptor.forClass(com.barterplatform.domain.identity.entity.PasswordResetTokenEntity.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        com.barterplatform.domain.identity.entity.PasswordResetTokenEntity saved = tokenCaptor.getValue();
        assertEquals(user.getId(), saved.getUserId());
        assertNotNull(saved.getTokenHash());
        assertNotNull(saved.getExpiresAt());

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailSender).sendHtml(eq("alex@example.com"), eq("Barter Platform – Reset your password"), htmlCaptor.capture());
        String html = htmlCaptor.getValue();

        Pattern p = Pattern.compile("token=([0-9a-fA-F]{64})");
        Matcher m = p.matcher(html);
        assertTrue(m.find());
        String rawToken = m.group(1);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String expectedHash = HexFormat.of().formatHex(digest.digest(rawToken.getBytes()));
        assertEquals(expectedHash, saved.getTokenHash());
    }

    @Test
    void forgotPassword_returnsGenericSuccessWhenUserDoesNotExist() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        MessageResponse response = authService.forgotPassword(new ForgotPasswordRequest("unknown@example.com"));

        assertEquals("If an account exists for this email, a password reset link has been sent.", response.getMessage());

        verify(passwordResetTokenRepository, never()).save(any());
        verify(mailSender, never()).sendHtml(any(), any(), any());
    }

    @Test
    void resetPassword_updatesEncodedPassword_and_marksTokenUsed() throws Exception {
        UserEntity user = activeUser();
        String rawToken = "0123456789abcdef0123456789abcdef";

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String tokenHash = HexFormat.of().formatHex(digest.digest(rawToken.getBytes()));

        com.barterplatform.domain.identity.entity.PasswordResetTokenEntity tokenEntity = new com.barterplatform.domain.identity.entity.PasswordResetTokenEntity();
        tokenEntity.setUserId(user.getId());
        tokenEntity.setTokenHash(tokenHash);
        tokenEntity.setExpiresAt(OffsetDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(tokenEntity));
        when(passwordEncoder.encode("NewP@ssword123")).thenReturn("encoded-new");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordResetTokenRepository.save(any(com.barterplatform.domain.identity.entity.PasswordResetTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = authService.resetPassword(new ResetPasswordRequest("alex@example.com", rawToken, "NewP@ssword123"));

        assertEquals("Password reset successfully.", response.getMessage());

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();
        assertEquals("encoded-new", savedUser.getPasswordHash());

        ArgumentCaptor<com.barterplatform.domain.identity.entity.PasswordResetTokenEntity> tokenSaveCaptor = ArgumentCaptor.forClass(com.barterplatform.domain.identity.entity.PasswordResetTokenEntity.class);
        verify(passwordResetTokenRepository).save(tokenSaveCaptor.capture());
        com.barterplatform.domain.identity.entity.PasswordResetTokenEntity savedToken = tokenSaveCaptor.getValue();
        assertNotNull(savedToken.getUsedAt());
    }

    @Test
    void resetPassword_rejectsExpiredToken() throws Exception {
        UserEntity user = activeUser();
        String rawToken = "0123456789abcdef0123456789abcdef";

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String tokenHash = HexFormat.of().formatHex(digest.digest(rawToken.getBytes()));

        com.barterplatform.domain.identity.entity.PasswordResetTokenEntity tokenEntity = new com.barterplatform.domain.identity.entity.PasswordResetTokenEntity();
        tokenEntity.setUserId(user.getId());
        tokenEntity.setTokenHash(tokenHash);
        tokenEntity.setExpiresAt(OffsetDateTime.now().minusMinutes(1));

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(tokenEntity));

        ApiException ex = assertThrows(ApiException.class, () -> authService.resetPassword(new ResetPasswordRequest("alex@example.com", rawToken, "NewP@ssword123")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    @Test
    void resetPassword_rejectsInvalidToken() throws Exception {
        UserEntity user = activeUser();
        String rawToken = "0123456789abcdef0123456789abcdef";

        com.barterplatform.domain.identity.entity.PasswordResetTokenEntity tokenEntity = new com.barterplatform.domain.identity.entity.PasswordResetTokenEntity();
        tokenEntity.setUserId(user.getId());
        tokenEntity.setTokenHash("deadbeef");
        tokenEntity.setExpiresAt(OffsetDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(tokenEntity));

        ApiException ex = assertThrows(ApiException.class, () -> authService.resetPassword(new ResetPasswordRequest("alex@example.com", rawToken, "NewP@ssword123")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(ErrorCode.BAD_REQUEST, ex.getCode());
    }

    private UserEntity activeUser() {
        UserEntity user = new UserEntity();
        user.setId(101L);
        user.setUuid(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        user.setUsername("alex99");
        user.setEmail("alex@example.com");
        user.setPasswordHash("$2a$10$hashedpassword");
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setMfaEnabled(false);
        user.setPreferredLanguage(PreferredLanguage.EN);
        user.setCreatedAt(OffsetDateTime.parse("2026-05-08T10:15:30Z"));
        return user;
    }

    private UserRoleEntity createTestUserRoleEntity(Long userId, Long roleId) {
        UserRoleId userRoleId = new UserRoleId();
        userRoleId.setUserId(userId);
        userRoleId.setRoleId(roleId);

        UserRoleEntity entity = new UserRoleEntity();
        entity.setId(userRoleId);
        entity.setAssignedAt(OffsetDateTime.parse("2026-05-08T10:15:30Z"));
        return entity;
    }
}
