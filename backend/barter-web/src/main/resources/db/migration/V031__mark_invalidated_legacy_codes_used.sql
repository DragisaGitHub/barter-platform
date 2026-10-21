-- V030 retired legacy SHA-256 codes through invalidated_at. Older application
-- versions only check used_at, so mirror that retirement for safe image rollback.
UPDATE email_verification_codes
SET used_at = invalidated_at
WHERE used_at IS NULL
  AND invalidated_at IS NOT NULL;
