-- Email verification codes table
CREATE TABLE IF NOT EXISTS email_verification_codes (
    id         BIGSERIAL PRIMARY KEY,
    uuid       UUID NOT NULL,
    user_id    BIGINT NOT NULL,
    code_hash  VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at    TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_email_verification_codes_uuid UNIQUE (uuid),
    CONSTRAINT fk_email_verification_codes_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_email_verification_codes_user_id ON email_verification_codes (user_id);
CREATE INDEX IF NOT EXISTS idx_email_verification_codes_expires_at ON email_verification_codes (expires_at);


