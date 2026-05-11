-- Notifications table
-- =============================================
-- notifications
-- =============================================
CREATE TABLE IF NOT EXISTS notifications (
    id                BIGSERIAL                NOT NULL,
    uuid              UUID                     NOT NULL,
    recipient_user_id BIGINT                   NOT NULL,
    type              VARCHAR(64)              NOT NULL,
    title             VARCHAR(255)             NOT NULL,
    message           TEXT,
    reference_uuid    UUID,
    reference_type    VARCHAR(64),
    is_read           BOOLEAN                  NOT NULL DEFAULT false,
    read_at           TIMESTAMP WITH TIME ZONE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT uq_notifications_uuid UNIQUE (uuid),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_user_id) REFERENCES users (id)
);

-- Index for listing notifications by user
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_user_id
    ON notifications (recipient_user_id);

-- Index for unread count queries
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_unread
    ON notifications (recipient_user_id, is_read);

-- Index for listing notifications by user ordered by creation time
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created
    ON notifications (recipient_user_id, created_at DESC);

