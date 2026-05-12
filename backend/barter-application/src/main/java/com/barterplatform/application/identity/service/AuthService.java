package com.barterplatform.application.identity.service;

import com.barterplatform.api.model.*;

import java.util.UUID;

public interface AuthService {

    CurrentUserResponse registerUser(RegisterUserRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refreshToken(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    CurrentUserResponse getCurrentUser(UUID userUuid);

    MessageResponse verifyEmail(VerifyEmailRequest request);

    MessageResponse resendVerificationCode(ResendVerificationCodeRequest request);

    MessageResponse forgotPassword(ForgotPasswordRequest request);

    MessageResponse resetPassword(ResetPasswordRequest request);
}
