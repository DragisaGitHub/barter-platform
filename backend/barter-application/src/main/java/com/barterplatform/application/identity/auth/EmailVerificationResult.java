package com.barterplatform.application.identity.auth;

public enum EmailVerificationResult {
    VERIFIED,
    ALREADY_VERIFIED,
    MISSING,
    INVALID,
    EXPIRED,
    LOCKED
}
