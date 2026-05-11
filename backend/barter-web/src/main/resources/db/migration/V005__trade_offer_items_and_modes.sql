-- V005: Trade offer items join table and trade modes

-- =============================================
-- 1. Add mode column to trade_offers
-- =============================================
ALTER TABLE trade_offers
    ADD COLUMN mode VARCHAR(32);

UPDATE trade_offers SET mode = 'ITEM_EXCHANGE' WHERE mode IS NULL;

ALTER TABLE trade_offers
    ALTER COLUMN mode SET NOT NULL;

ALTER TABLE trade_offers
    ADD CONSTRAINT ck_trade_offers_mode CHECK (mode IN ('ITEM_EXCHANGE', 'GIFT', 'NEGOTIABLE'));

-- =============================================
-- 2. Make sender_item_id nullable (GIFT / NEGOTIABLE may have no offered item)
-- =============================================
ALTER TABLE trade_offers
    ALTER COLUMN sender_item_id DROP NOT NULL;

-- Drop the constraint that requires sender != receiver item (sender item can now be null)
ALTER TABLE trade_offers
    DROP CONSTRAINT IF EXISTS ck_trade_offers_different_items;

-- Re-add with NULL-safe check
ALTER TABLE trade_offers
    ADD CONSTRAINT ck_trade_offers_different_items CHECK (sender_item_id IS NULL OR sender_item_id <> receiver_item_id);

-- =============================================
-- 3. Create trade_offer_items table
-- =============================================
CREATE TABLE IF NOT EXISTS trade_offer_items (
    id              BIGSERIAL PRIMARY KEY,
    trade_offer_id  BIGINT                   NOT NULL,
    item_id         BIGINT                   NOT NULL,
    side            VARCHAR(16)              NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_trade_offer_items_offer FOREIGN KEY (trade_offer_id) REFERENCES trade_offers (id) ON DELETE CASCADE,
    CONSTRAINT fk_trade_offer_items_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT ck_trade_offer_items_side CHECK (side IN ('OFFERED', 'REQUESTED'))
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_trade_offer_items_trade_offer_id ON trade_offer_items (trade_offer_id);
CREATE INDEX IF NOT EXISTS idx_trade_offer_items_item_id ON trade_offer_items (item_id);
CREATE INDEX IF NOT EXISTS idx_trade_offer_items_side ON trade_offer_items (side);
CREATE INDEX IF NOT EXISTS idx_trade_offer_items_item_side ON trade_offer_items (item_id, side);

-- Prevent duplicate item per offer per side
CREATE UNIQUE INDEX IF NOT EXISTS uq_trade_offer_items_offer_item_side ON trade_offer_items (trade_offer_id, item_id, side);

-- =============================================
-- 4. Backfill existing data into trade_offer_items
-- =============================================
-- Backfill sender items as OFFERED
INSERT INTO trade_offer_items (trade_offer_id, item_id, side, created_at)
SELECT id, sender_item_id, 'OFFERED', created_at
FROM trade_offers
WHERE sender_item_id IS NOT NULL;

-- Backfill receiver items as REQUESTED
INSERT INTO trade_offer_items (trade_offer_id, item_id, side, created_at)
SELECT id, receiver_item_id, 'REQUESTED', created_at
FROM trade_offers;

-- =============================================
-- 5. Drop old partial unique index on sender_item_id (no longer valid for multi-item)
-- =============================================
DROP INDEX IF EXISTS uq_trade_offers_accepted_sender_item;

-- Keep accepted receiver item uniqueness
-- (already exists: uq_trade_offers_accepted_receiver_item)

-- Drop old pending pair unique index (no longer valid for multi-item offers)
DROP INDEX IF EXISTS uq_trade_offers_pending_pair;

-- Add index on mode
CREATE INDEX IF NOT EXISTS idx_trade_offers_mode ON trade_offers (mode);

