-- Trade offers domain schema

-- =============================================
-- trade_offers
-- =============================================
CREATE TABLE IF NOT EXISTS trade_offers (
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID                     NOT NULL,
    sender_user_id   BIGINT                   NOT NULL,
    receiver_user_id BIGINT                   NOT NULL,
    sender_item_id   BIGINT                   NOT NULL,
    receiver_item_id BIGINT                   NOT NULL,
    status           VARCHAR(32)              NOT NULL,
    message          TEXT,
    responded_at     TIMESTAMP WITH TIME ZONE,
    expires_at       TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE,
    deleted_at       TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_trade_offers_uuid UNIQUE (uuid),
    CONSTRAINT fk_trade_offers_sender_user FOREIGN KEY (sender_user_id) REFERENCES users (id),
    CONSTRAINT fk_trade_offers_receiver_user FOREIGN KEY (receiver_user_id) REFERENCES users (id),
    CONSTRAINT fk_trade_offers_sender_item FOREIGN KEY (sender_item_id) REFERENCES items (id),
    CONSTRAINT fk_trade_offers_receiver_item FOREIGN KEY (receiver_item_id) REFERENCES items (id),
    CONSTRAINT ck_trade_offers_no_self_trade CHECK (sender_user_id <> receiver_user_id),
    CONSTRAINT ck_trade_offers_different_items CHECK (sender_item_id <> receiver_item_id),
    CONSTRAINT ck_trade_offers_message_length CHECK (message IS NULL OR LENGTH(message) <= 1000),
    CONSTRAINT ck_trade_offers_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'EXPIRED'))
);

-- Single-column indexes
CREATE INDEX IF NOT EXISTS idx_trade_offers_sender_user_id ON trade_offers (sender_user_id);
CREATE INDEX IF NOT EXISTS idx_trade_offers_receiver_user_id ON trade_offers (receiver_user_id);
CREATE INDEX IF NOT EXISTS idx_trade_offers_sender_item_id ON trade_offers (sender_item_id);
CREATE INDEX IF NOT EXISTS idx_trade_offers_receiver_item_id ON trade_offers (receiver_item_id);
CREATE INDEX IF NOT EXISTS idx_trade_offers_status ON trade_offers (status);
CREATE INDEX IF NOT EXISTS idx_trade_offers_created_at ON trade_offers (created_at);

-- Composite indexes for list queries
CREATE INDEX IF NOT EXISTS idx_trade_offers_receiver_status ON trade_offers (receiver_user_id, status);
CREATE INDEX IF NOT EXISTS idx_trade_offers_sender_status ON trade_offers (sender_user_id, status);

-- Partial unique indexes: only one ACCEPTED offer per item
CREATE UNIQUE INDEX IF NOT EXISTS uq_trade_offers_accepted_sender_item ON trade_offers (sender_item_id) WHERE status = 'ACCEPTED';
CREATE UNIQUE INDEX IF NOT EXISTS uq_trade_offers_accepted_receiver_item ON trade_offers (receiver_item_id) WHERE status = 'ACCEPTED';

-- Partial unique index: prevent duplicate pending offers for the same item pair from the same sender
CREATE UNIQUE INDEX IF NOT EXISTS uq_trade_offers_pending_pair ON trade_offers (sender_user_id, sender_item_id, receiver_item_id) WHERE status = 'PENDING';

