-- Beta feedback inbox persistence

CREATE TABLE beta_feedback (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    user_uuid UUID NOT NULL,
    username VARCHAR(80) NOT NULL,
    email VARCHAR(255),
    category VARCHAR(64) NOT NULL,
    message TEXT NOT NULL,
    source_page VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    reviewed_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_beta_feedback_category CHECK (
        category IN (
            'ONBOARDING',
            'LISTINGS',
            'MARKETPLACE',
            'OFFERS',
            'TRUST_AND_SAFETY',
            'GENERAL'
        )
    ),
    CONSTRAINT ck_beta_feedback_status CHECK (status IN ('NEW', 'REVIEWED', 'RESOLVED')),
    CONSTRAINT ck_beta_feedback_message_length CHECK (char_length(message) BETWEEN 20 AND 2000),
    CONSTRAINT ck_beta_feedback_source_page_length CHECK (source_page IS NULL OR char_length(source_page) <= 255),
    CONSTRAINT ck_beta_feedback_reviewed_when_status CHECK (
        status = 'NEW'
        OR reviewed_at IS NOT NULL
    ),
    CONSTRAINT ck_beta_feedback_resolved_when_status CHECK (
        status <> 'RESOLVED'
        OR resolved_at IS NOT NULL
    )
);

CREATE INDEX idx_beta_feedback_status_created_at_desc
    ON beta_feedback (status, created_at DESC);

CREATE INDEX idx_beta_feedback_created_at_desc
    ON beta_feedback (created_at DESC);

CREATE INDEX idx_beta_feedback_user_uuid_created_at_desc
    ON beta_feedback (user_uuid, created_at DESC);

