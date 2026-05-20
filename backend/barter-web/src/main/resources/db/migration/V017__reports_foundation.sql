-- Reports moderation foundation

CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    reporter_user_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_uuid UUID NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    details TEXT,
    status VARCHAR(32) NOT NULL,
    assigned_moderator_user_id BIGINT,
    resolution_note TEXT,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_reports_reporter_user FOREIGN KEY (reporter_user_id) REFERENCES users (id),
    CONSTRAINT fk_reports_assigned_moderator_user FOREIGN KEY (assigned_moderator_user_id) REFERENCES users (id),
    CONSTRAINT ck_reports_target_type CHECK (target_type IN ('ITEM', 'USER', 'MESSAGE', 'TRADE_OFFER', 'REVIEW')),
    CONSTRAINT ck_reports_reason_code CHECK (
        reason_code IN (
            'PROHIBITED_ITEM',
            'SPAM_SCAM',
            'HARASSMENT',
            'MISLEADING_LISTING',
            'UNSAFE_EXCHANGE',
            'NO_SHOW',
            'OTHER'
        )
    ),
    CONSTRAINT ck_reports_status CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'DISMISSED')),
    CONSTRAINT ck_reports_details_length CHECK (details IS NULL OR char_length(details) <= 2000),
    CONSTRAINT ck_reports_resolution_note_length CHECK (resolution_note IS NULL OR char_length(resolution_note) <= 2000)
);

CREATE INDEX idx_reports_status_created_at_desc
    ON reports (status, created_at DESC);

CREATE INDEX idx_reports_target_type_created_at_desc
    ON reports (target_type, created_at DESC);

CREATE INDEX idx_reports_target_uuid
    ON reports (target_uuid);

CREATE INDEX idx_reports_reporter_user_status
    ON reports (reporter_user_id, status);

CREATE INDEX idx_reports_assigned_moderator_status
    ON reports (assigned_moderator_user_id, status);

