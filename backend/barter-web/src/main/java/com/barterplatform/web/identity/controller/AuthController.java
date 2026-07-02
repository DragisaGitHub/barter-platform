package com.barterplatform.web.identity.controller;

import com.barterplatform.api.controller.AuthApi;
import com.barterplatform.api.model.*;
import com.barterplatform.application.identity.service.AuthService;
import com.barterplatform.web.security.jwt.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<CurrentUserResponse> registerUser(RegisterUserRequest registerUserRequest) {
        CurrentUserResponse response = authService.registerUser(registerUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<TokenResponse> login(LoginRequest loginRequest) {
        TokenResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<MessageResponse> forgotPassword(ForgotPasswordRequest forgotPasswordRequest) {
        MessageResponse response = authService.forgotPassword(forgotPasswordRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<MessageResponse> resetPassword(ResetPasswordRequest resetPasswordRequest) {
        MessageResponse response = authService.resetPassword(resetPasswordRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TokenResponse> refreshToken(RefreshTokenRequest refreshTokenRequest) {
        TokenResponse response = authService.refreshToken(refreshTokenRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> logout(RefreshTokenRequest refreshTokenRequest) {
        authService.logout(refreshTokenRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        AuthenticatedUser principal = (AuthenticatedUser) Objects.requireNonNull(SecurityContextHolder.getContext()
                .getAuthentication()).getPrincipal();
        assert principal != null;
        CurrentUserResponse response = authService.getCurrentUser(principal.getUserUuid());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<VerifyEmailResponse> verifyEmail(VerifyEmailRequest verifyEmailRequest) {
        VerifyEmailResponse response = authService.verifyEmail(verifyEmailRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<MessageResponse> resendVerificationCode(
            ResendVerificationCodeRequest resendVerificationCodeRequest) {
        MessageResponse response = authService.resendVerificationCode(resendVerificationCodeRequest);
        return ResponseEntity.ok(response);
    }
}
