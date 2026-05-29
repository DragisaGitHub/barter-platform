-- Saved catalog searches for authenticated users

CREATE TABLE IF NOT EXISTS saved_searches (
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID                     NOT NULL,
    user_id          BIGINT                   NOT NULL,
    name             VARCHAR(120)             NOT NULL,
    criteria_payload TEXT                     NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_saved_searches_uuid UNIQUE (uuid),
    CONSTRAINT fk_saved_searches_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_saved_searches_user_name_ci ON saved_searches (user_id, lower(name));
CREATE INDEX IF NOT EXISTS idx_saved_searches_user_created_at ON saved_searches (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_saved_searches_user_updated_at ON saved_searches (user_id, updated_at DESC);

