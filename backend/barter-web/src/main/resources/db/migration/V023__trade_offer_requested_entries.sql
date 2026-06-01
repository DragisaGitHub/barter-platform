CREATE TABLE IF NOT EXISTS trade_offer_requested_entries (
    id                    BIGSERIAL PRIMARY KEY,
    trade_offer_id        BIGINT                   NOT NULL,
    item_listing_entry_id BIGINT                   NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_trade_offer_requested_entries_offer
        FOREIGN KEY (trade_offer_id) REFERENCES trade_offers (id) ON DELETE CASCADE,
    CONSTRAINT fk_trade_offer_requested_entries_entry
        FOREIGN KEY (item_listing_entry_id) REFERENCES item_listing_entries (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_trade_offer_requested_entries_offer_id
    ON trade_offer_requested_entries (trade_offer_id);
CREATE INDEX IF NOT EXISTS idx_trade_offer_requested_entries_entry_id
    ON trade_offer_requested_entries (item_listing_entry_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_trade_offer_requested_entries_offer_entry
    ON trade_offer_requested_entries (trade_offer_id, item_listing_entry_id);

