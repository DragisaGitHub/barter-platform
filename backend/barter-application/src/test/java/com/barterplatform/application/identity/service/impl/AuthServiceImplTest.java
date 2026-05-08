package com.barterplatform.application.identity.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.barterplatform.api.model.CurrentUserResponse;
import com.barterplatform.api.model.LoginRequest;
import com.barterplatform.api.model.RefreshTokenRequest;
import com.barterplatform.api.model.RegisterUserRequest;
import com.barterplatform.api.model.RoleResponse;
import com.barterplatform.api.model.TokenResponse;
import com.barterplatform.api.model.UserStatus;
import com.barterplatform.application.identity.auth.JwtService;
import com.barterplatform.application.identity.auth.RefreshTokenService;
import com.barterplatform.application.identity.mapper.RoleMapper;
import com.barterplatform.application.identity.mapper.UserMapper;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.domain.identity.entity.RefreshTokenEntity;
import com.barterplatform.domain.identity.entity.RoleEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.entity.UserRoleEntity;
import com.barterplatform.domain.identity.entity.UserRoleId;
import com.barterplatform.domain.identity.enums.RoleCode;
import com.barterplatform.infrastructure.identity.repository.RoleRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @InjectMocks
    private AuthServiceImpl authService;

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

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
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
        assertFalse(response.getEmailVerified());
        assertFalse(response.getMfaEnabled());
        assertEquals(1, response.getRoles().size());
        assertSame(userRoleResponse, response.getRoles().getFirst());
        assertEquals(0, response.getPermissions().size());
        assertEquals(0, response.getOauthAccounts().size());
        assertNull(response.getMfaSettings());

        verify(roleRepository).findByCode(RoleCode.USER);
        verify(userMapper).toCurrentUserResponse(any(UserEntity.class));
        verify(roleMapper).toResponse(userRole);
    }

    @Test
    void shouldThrowConflictWhenEmailAlreadyExists() {
        RegisterUserRequest request = new RegisterUserRequest("alex99", "alex@example.com", "P@ssword123");
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> authService.registerUser(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(ErrorCode.CONFLICT, exception.getCode());
        assertEquals("Email 'alex@example.com' is already in use.", exception.getMessage());

        verify(userRepository, never()).existsByUsername(any());
        verifyNoInteractions(roleRepository, userRoleRepository, userMapper, roleMapper, passwordEncoder);
    }

    @Test
    void shouldThrowConflictWhenUsernameAlreadyExists() {
        RegisterUserRequest request = new RegisterUserRequest("alex99", "alex@example.com", "P@ssword123");
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

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

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(user));
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
                new CurrentUserResponse().uuid(user.getUuid()).username(user.getUsername()).email(user.getEmail()));
        when(roleMapper.toResponseList(anyList())).thenReturn(List.of());

        TokenResponse response = authService.login(request);

        assertEquals("jwt-access-token", response.getAccessToken());
        assertEquals("raw-refresh-token", response.getRefreshToken());
        assertEquals(TokenResponse.TokenTypeEnum.BEARER, response.getTokenType());
        assertEquals(1800L, response.getExpiresIn());
        assertNotNull(response.getUser());

        verify(userRepository).findByEmail("alex@example.com");
        verify(passwordEncoder).matches("P@ssword123", user.getPasswordHash());
    }

    @Test
    void shouldLoginSuccessfullyWithUsername() {
        LoginRequest request = new LoginRequest("alex99", "P@ssword123");
        UserEntity user = activeUser();
        RoleEntity role = userRoleEntity();
        UserRoleEntity userRoleEntity = createTestUserRoleEntity(user.getId(), role.getId());

        when(userRepository.findByUsername("alex99")).thenReturn(Optional.of(user));
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
        verify(userRepository).findByUsername("alex99");
    }

    @Test
    void shouldThrowUnauthorizedForInvalidEmail() {
        LoginRequest request = new LoginRequest("unknown@example.com", "P@ssword123");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
        assertEquals("Invalid credentials.", ex.getMessage());
    }

    @Test
    void shouldThrowUnauthorizedForWrongPassword() {
        LoginRequest request = new LoginRequest("alex@example.com", "wrong-password");
        UserEntity user = activeUser();

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(user));
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

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(user));
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

        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.of(user));
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
                new CurrentUserResponse().uuid(user.getUuid()).username(user.getUsername()));
        when(roleMapper.toResponseList(anyList())).thenReturn(List.of());

        TokenResponse response = authService.refreshToken(request);

        assertEquals("new-jwt-access-token", response.getAccessToken());
        assertEquals("new-raw-refresh-token", response.getRefreshToken());
        assertEquals(TokenResponse.TokenTypeEnum.BEARER, response.getTokenType());
        assertEquals(1800L, response.getExpiresIn());
        assertEquals(604800L, response.getRefreshExpiresIn());
        assertNotNull(response.getUser());

        verify(refreshTokenService).revoke(existingToken);
        verify(refreshTokenService).createRefreshToken(user.getId());
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
