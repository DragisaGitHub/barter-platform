package com.barterplatform.application.identity.service;

import com.barterplatform.api.model.MessageResponse;
import com.barterplatform.api.model.ResendVerificationCodeRequest;
import com.barterplatform.api.model.VerifyEmailRequest;
import com.barterplatform.application.identity.auth.EmailVerificationResult;

public interface EmailVerificationService {

    /**
     * Generate a verification code for the given user and send it via mail.
     */
    void createAndSendVerificationCode(Long userId, String email);

    /**
     * Verify the email with the provided code. Activates the user on success.
     */
    EmailVerificationResult verifyEmail(VerifyEmailRequest request);

    /**
     * Resend a new verification code to the user.
     */
    MessageResponse resendVerificationCode(ResendVerificationCodeRequest request);
}

