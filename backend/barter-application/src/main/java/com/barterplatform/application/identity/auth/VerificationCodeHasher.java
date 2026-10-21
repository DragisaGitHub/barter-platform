package com.barterplatform.application.identity.auth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VerificationCodeHasher {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    public static final int MIN_DEDICATED_SECRET_LENGTH = 32;
    private static final byte[] CONTEXT = "barter-platform:email-verification-code:v1"
            .getBytes(StandardCharsets.UTF_8);

    private final byte[] key;

    public VerificationCodeHasher(
            @Value("${barter.verification.code-secret:}") String dedicatedSecret,
            @Value("${barter.jwt.secret:}") String jwtSecret) {
        String masterSecret = dedicatedSecret == null || dedicatedSecret.isBlank()
                ? jwtSecret
                : dedicatedSecret;
        if (masterSecret == null || masterSecret.isBlank()) {
            throw new IllegalStateException("A verification HMAC secret or JWT secret must be configured.");
        }
        if (dedicatedSecret != null && !dedicatedSecret.isBlank() && !isValidDedicatedSecret(dedicatedSecret)) {
            throw new IllegalStateException("barter.verification.code-secret must contain at least 32 printable ASCII characters.");
        }
        this.key = deriveKey(masterSecret.trim());
    }

    public static boolean isValidDedicatedSecret(String secret) {
        if (secret == null || secret.length() < MIN_DEDICATED_SECRET_LENGTH) {
            return false;
        }
        for (int i = 0; i < secret.length(); i++) {
            char value = secret.charAt(i);
            if (value < 0x21 || value > 0x7e) {
                return false;
            }
        }
        return true;
    }

    public String hash(Long userId, String code) {
        if (userId == null || code == null) {
            throw new IllegalArgumentException("userId and code are required.");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] digest = mac.doFinal((userId + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable.", e);
        }
    }

    public boolean matches(Long userId, String code, String expectedHash) {
        if (userId == null || code == null || expectedHash == null) {
            return false;
        }
        byte[] expected;
        try {
            expected = HexFormat.of().parseHex(expectedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
        byte[] actual = HexFormat.of().parseHex(hash(userId, code));
        return MessageDigest.isEqual(actual, expected);
    }

    private static byte[] deriveKey(String masterSecret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(masterSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(CONTEXT);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable.", e);
        }
    }
}
