-- User preferred language foundation

ALTER TABLE users
    ADD COLUMN preferred_language VARCHAR(2) DEFAULT 'SR';

UPDATE users
SET preferred_language = 'SR'
WHERE preferred_language IS NULL;

ALTER TABLE users
    ALTER COLUMN preferred_language SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT ck_users_preferred_language
        CHECK (preferred_language IN ('SR', 'EN'));

