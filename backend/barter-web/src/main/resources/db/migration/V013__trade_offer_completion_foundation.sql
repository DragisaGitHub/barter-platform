-- Trade offer completion foundation

ALTER TABLE trade_offers
    ADD COLUMN sender_completed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN receiver_completed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN completed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE trade_offers
    DROP CONSTRAINT IF EXISTS ck_trade_offers_status;

ALTER TABLE trade_offers
    ADD CONSTRAINT ck_trade_offers_status CHECK (
        status IN ('PENDING', 'ACCEPTED', 'COMPLETED', 'REJECTED', 'CANCELLED', 'EXPIRED', 'INVALIDATED')
    );

UPDATE trade_offers
SET status = 'COMPLETED',
    sender_completed_at = COALESCE(responded_at, updated_at, created_at),
    receiver_completed_at = COALESCE(responded_at, updated_at, created_at),
    completed_at = COALESCE(responded_at, updated_at, created_at)
WHERE status = 'ACCEPTED';

ALTER TABLE trade_offers
    ADD CONSTRAINT ck_trade_offers_completed_timestamps CHECK (
        status <> 'COMPLETED'
        OR (
            sender_completed_at IS NOT NULL
            AND receiver_completed_at IS NOT NULL
            AND completed_at IS NOT NULL
        )
    );

