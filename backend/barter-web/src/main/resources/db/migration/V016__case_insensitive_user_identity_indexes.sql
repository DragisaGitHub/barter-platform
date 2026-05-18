-- Enforce case-insensitive uniqueness for login identifiers while preserving original casing.
-- Existing case-sensitive unique constraints remain; these functional indexes prevent
-- values such as 'Dragisa' and 'dragisa' from coexisting.
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username_lower ON users (lower(username));
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_lower ON users (lower(email));

