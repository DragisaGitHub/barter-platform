ALTER TABLE email_verification_codes
    ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN invalidated_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN counts_toward_issuance_limit BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE email_verification_codes
    ADD CONSTRAINT chk_email_verification_codes_failed_attempts_nonnegative
        CHECK (failed_attempts >= 0);

-- Existing code_hash values are unkeyed SHA-256 hashes and cannot be verified
-- after switching to HMAC-SHA256. Retire active rows; users can request a new code.
UPDATE email_verification_codes
SET invalidated_at = CURRENT_TIMESTAMP
WHERE used_at IS NULL
  AND invalidated_at IS NULL;

CREATE INDEX idx_email_verification_codes_user_created_at
    ON email_verification_codes (user_id, created_at DESC)
    WHERE counts_toward_issuance_limit = TRUE;

CREATE INDEX idx_email_verification_codes_active_user_created_at
    ON email_verification_codes (user_id, created_at DESC)
    WHERE used_at IS NULL AND invalidated_at IS NULL;
