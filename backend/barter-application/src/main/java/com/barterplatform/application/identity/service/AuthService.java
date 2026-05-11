package com.barterplatform.application.identity.service;

import com.barterplatform.api.model.CurrentUserResponse;
import com.barterplatform.api.model.LoginRequest;
import com.barterplatform.api.model.MessageResponse;
import com.barterplatform.api.model.RefreshTokenRequest;
import com.barterplatform.api.model.RegisterUserRequest;
import com.barterplatform.api.model.ResendVerificationCodeRequest;
import com.barterplatform.api.model.TokenResponse;
import com.barterplatform.api.model.VerifyEmailRequest;
import java.util.UUID;

public interface AuthService {

    CurrentUserResponse registerUser(RegisterUserRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refreshToken(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    CurrentUserResponse getCurrentUser(UUID userUuid);

    MessageResponse verifyEmail(VerifyEmailRequest request);

    MessageResponse resendVerificationCode(ResendVerificationCodeRequest request);
}
