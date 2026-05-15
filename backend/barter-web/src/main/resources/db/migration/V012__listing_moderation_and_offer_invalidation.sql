-- Listing moderation foundation and trade offer invalidation

CREATE TABLE IF NOT EXISTS listing_moderation_actions (
    id                   BIGSERIAL PRIMARY KEY,
    uuid                 UUID                     NOT NULL,
    item_id              BIGINT                   NOT NULL,
    action_type          VARCHAR(32)              NOT NULL,
    reason_code          VARCHAR(64)              NOT NULL,
    source_type          VARCHAR(32)              NOT NULL,
    performed_by_user_id BIGINT,
    user_message         TEXT,
    internal_note        TEXT,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_listing_moderation_actions_uuid UNIQUE (uuid),
    CONSTRAINT fk_listing_moderation_actions_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT fk_listing_moderation_actions_actor FOREIGN KEY (performed_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_listing_moderation_actions_type CHECK (action_type IN ('REMOVE', 'RESTORE')),
    CONSTRAINT ck_listing_moderation_actions_reason CHECK (
        reason_code IN (
            'POLICY_VIOLATION',
            'PROHIBITED_ITEM',
            'MISLEADING_CONTENT',
            'DUPLICATE_LISTING',
            'SPAM',
            'SAFETY_CONCERN',
            'OWNER_REQUEST',
            'OTHER'
        )
    ),
    CONSTRAINT ck_listing_moderation_actions_source CHECK (source_type IN ('ADMIN', 'SYSTEM')),
    CONSTRAINT ck_listing_moderation_actions_user_message_length CHECK (user_message IS NULL OR LENGTH(user_message) <= 1000),
    CONSTRAINT ck_listing_moderation_actions_internal_note_length CHECK (internal_note IS NULL OR LENGTH(internal_note) <= 2000)
);

CREATE INDEX IF NOT EXISTS idx_listing_moderation_actions_item_id_created_at
    ON listing_moderation_actions (item_id, created_at DESC);

ALTER TABLE items
    DROP CONSTRAINT IF EXISTS ck_items_status;

ALTER TABLE items
    ADD CONSTRAINT ck_items_status CHECK (status IN ('DRAFT', 'ACTIVE', 'RESERVED', 'ARCHIVED', 'REMOVED'));

ALTER TABLE trade_offers
    DROP CONSTRAINT IF EXISTS ck_trade_offers_status;

ALTER TABLE trade_offers
    ADD CONSTRAINT ck_trade_offers_status CHECK (
        status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'EXPIRED', 'INVALIDATED')
    );

