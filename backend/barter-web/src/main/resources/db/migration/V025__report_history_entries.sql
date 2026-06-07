CREATE TABLE report_history_entries
(
    id                                  BIGSERIAL PRIMARY KEY,
    uuid                                UUID                     NOT NULL UNIQUE,
    report_id                           BIGINT                   NOT NULL,
    actor_user_id                       BIGINT,
    event_type                          VARCHAR(48)              NOT NULL,
    previous_status                     VARCHAR(32),
    new_status                          VARCHAR(32),
    previous_assigned_moderator_user_id BIGINT,
    new_assigned_moderator_user_id      BIGINT,
    note                                TEXT,
    created_at                          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_report_history_entries_report
        FOREIGN KEY (report_id) REFERENCES reports (id) ON DELETE CASCADE,

    CONSTRAINT fk_report_history_entries_actor_user
        FOREIGN KEY (actor_user_id) REFERENCES users (id),

    CONSTRAINT fk_report_history_entries_previous_assigned_moderator
        FOREIGN KEY (previous_assigned_moderator_user_id) REFERENCES users (id),

    CONSTRAINT fk_report_history_entries_new_assigned_moderator
        FOREIGN KEY (new_assigned_moderator_user_id) REFERENCES users (id),

    CONSTRAINT chk_report_history_entries_event_type
        CHECK (event_type IN (
                              'REPORT_CREATED',
                              'ASSIGNED',
                              'UNASSIGNED',
                              'STATUS_CHANGED',
                              'RESOLUTION_NOTE_CHANGED'
            )),

    CONSTRAINT chk_report_history_entries_previous_status
        CHECK (previous_status IS NULL OR previous_status IN (
                                                              'OPEN',
                                                              'IN_REVIEW',
                                                              'RESOLVED',
                                                              'DISMISSED'
            )),

    CONSTRAINT chk_report_history_entries_new_status
        CHECK (new_status IS NULL OR new_status IN (
                                                    'OPEN',
                                                    'IN_REVIEW',
                                                    'RESOLVED',
                                                    'DISMISSED'
            ))
);

CREATE INDEX idx_report_history_entries_report_created_at_desc
    ON report_history_entries (report_id, created_at DESC, id DESC);