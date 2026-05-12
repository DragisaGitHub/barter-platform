package com.barterplatform.web.identity.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.api.model.*;
import com.barterplatform.application.identity.service.AuthService;
import com.barterplatform.common.exception.ApiException;
import com.barterplatform.common.exception.ErrorCode;
import com.barterplatform.web.exception.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerMvcTest {

    private static final String REGISTER_REQUEST_JSON = """
            {
              "username": "alex99",
              "email": "alex@example.com",
              "password": "P@ssword123"
            }
            """;

    private static final String LOGIN_REQUEST_JSON = """
            {
              "identifier": "alex@example.com",
              "password": "P@ssword123"
            }
            """;

    private static final String FORGOT_PASSWORD_REQUEST_JSON = """
        {
          "email": "alex@example.com"
        }
        """;

    private static final String RESET_PASSWORD_REQUEST_JSON = """
        {
          "email": "alex@example.com",
          "token": "0123456789abcdef0123456789abcdef",
          "newPassword": "NewP@ssword123"
        }
        """;

    private static final String REFRESH_TOKEN_REQUEST_JSON = """
            {
              "refreshToken": "some-refresh-token"
            }
            """;

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldRegisterUserAndReturnCreated() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest("alex99", "alex@example.com", "P@ssword123");
        CurrentUserResponse response = currentUserResponse();

        when(authService.registerUser(request)).thenReturn(response);

        mockMvc.perform(registerRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_REQUEST_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.username").value("alex99"))
                .andExpect(jsonPath("$.email").value("alex@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.mfaEnabled").value(false))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.oauthAccounts").isArray())
                .andExpect(jsonPath("$.createdAt").exists());

        verify(authService).registerUser(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    void shouldMapDuplicateConflictFromService() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest("alex99", "alex@example.com", "P@ssword123");
        when(authService.registerUser(request)).thenThrow(
                new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "Email 'alex@example.com' is already in use."));

        mockMvc.perform(registerRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_REQUEST_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Email 'alex@example.com' is already in use."))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"));

        verify(authService).registerUser(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    void shouldLoginSuccessfullyAndReturnTokenResponse() throws Exception {
        LoginRequest request = new LoginRequest("alex@example.com", "P@ssword123");
        TokenResponse tokenResponse = tokenResponse();

        when(authService.login(request)).thenReturn(tokenResponse);

        mockMvc.perform(loginRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("raw-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.uuid").value("11111111-1111-1111-1111-111111111111"));

        verify(authService).login(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    void shouldRequestPasswordResetSuccessfully() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("alex@example.com");
        MessageResponse response = new MessageResponse()
                .message("If an account exists for this email, a password reset link has been sent.");

        when(authService.forgotPassword(request)).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contextPath("/api/v1")
                        .servletPath("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FORGOT_PASSWORD_REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account exists for this email, a password reset link has been sent."));

        verify(authService).forgotPassword(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    void shouldResetPasswordSuccessfully() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "alex@example.com",
                "0123456789abcdef0123456789abcdef",
                "NewP@ssword123");
        MessageResponse response = new MessageResponse()
                .message("Password reset successfully.");

        when(authService.resetPassword(request)).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contextPath("/api/v1")
                        .servletPath("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESET_PASSWORD_REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully."));

        verify(authService).resetPassword(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    void shouldReturnBadRequestForInvalidResetToken() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "alex@example.com",
                "0123456789abcdef0123456789abcdef",
                "NewP@ssword123");

        when(authService.resetPassword(request)).thenThrow(
                new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Invalid or expired password reset token."));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contextPath("/api/v1")
                        .servletPath("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESET_PASSWORD_REQUEST_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid or expired password reset token."));

        verify(authService).resetPassword(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    void shouldReturnBadRequestForExpiredResetToken() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "alex@example.com",
                "0123456789abcdef0123456789abcdef",
                "NewP@ssword123");

        when(authService.resetPassword(request)).thenThrow(
                new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Password reset token expired."));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contextPath("/api/v1")
                        .servletPath("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RESET_PASSWORD_REQUEST_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Password reset token expired."));

        verify(authService).resetPassword(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("alex@example.com", "P@ssword123");
        when(authService.login(request)).thenThrow(
                new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Invalid credentials."));

        mockMvc.perform(loginRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_REQUEST_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid credentials."));

        verify(authService).login(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    void shouldReturnForbiddenForSuspendedUser() throws Exception {
        LoginRequest request = new LoginRequest("alex@example.com", "P@ssword123");
        when(authService.login(request)).thenThrow(
                new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Account is suspended."));

        mockMvc.perform(loginRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_REQUEST_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Account is suspended."));

        verify(authService).login(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    void shouldRefreshTokenSuccessfully() throws Exception {
        TokenResponse tokenResponse = tokenResponse();
        RefreshTokenRequest request = new RefreshTokenRequest().refreshToken("some-refresh-token");

        when(authService.refreshToken(request)).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contextPath("/api/v1")
                        .servletPath("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REFRESH_TOKEN_REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("raw-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800));

        verify(authService).refreshToken(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    void shouldReturnUnauthorizedForInvalidRefreshToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest().refreshToken("some-refresh-token");
        when(authService.refreshToken(request)).thenThrow(
                new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Invalid refresh token."));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contextPath("/api/v1")
                        .servletPath("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REFRESH_TOKEN_REQUEST_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid refresh token."));

        verify(authService).refreshToken(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    void shouldLogoutSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contextPath("/api/v1")
                        .servletPath("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REFRESH_TOKEN_REQUEST_JSON))
                .andExpect(status().isNoContent());

        RefreshTokenRequest request = new RefreshTokenRequest().refreshToken("some-refresh-token");
        verify(authService).logout(request);
        verifyNoMoreInteractions(authService);
    }

    private MockHttpServletRequestBuilder registerRequest() {
        return post("/api/v1/auth/register")
                .contextPath("/api/v1")
                .servletPath("/auth/register");
    }

    private MockHttpServletRequestBuilder loginRequest() {
        return post("/api/v1/auth/login")
                .contextPath("/api/v1")
                .servletPath("/auth/login");
    }

    private CurrentUserResponse currentUserResponse() {
        return new CurrentUserResponse()
                .uuid(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .username("alex99")
                .email("alex@example.com")
                .status(UserStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .mfaEnabled(false)
                .roles(List.of())
                .permissions(List.of())
                .oauthAccounts(List.of())
                .createdAt(OffsetDateTime.parse("2026-05-08T10:15:30Z"));
    }

    private TokenResponse tokenResponse() {
        return new TokenResponse()
                .accessToken("jwt-access-token")
                .refreshToken("raw-refresh-token")
                .tokenType(TokenResponse.TokenTypeEnum.BEARER)
                .expiresIn(1800L)
                .refreshExpiresIn(604800L)
                .user(new CurrentUserResponse()
                        .uuid(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                        .username("alex99")
                        .email("alex@example.com")
                        .status(UserStatus.ACTIVE)
                        .emailVerified(true)
                        .mfaEnabled(false)
                        .roles(List.of())
                        .permissions(List.of())
                        .oauthAccounts(List.of())
                        .createdAt(OffsetDateTime.parse("2026-05-08T10:15:30Z")));
    }
}

