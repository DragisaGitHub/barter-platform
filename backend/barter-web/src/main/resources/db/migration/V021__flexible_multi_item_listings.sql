ALTER TABLE items
    ADD COLUMN IF NOT EXISTS listing_mode VARCHAR(40) NOT NULL DEFAULT 'SINGLE';

CREATE TABLE IF NOT EXISTS item_listing_entries (
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID                     NOT NULL,
    item_id     BIGINT                   NOT NULL,
    title       VARCHAR(200)             NOT NULL,
    description TEXT,
    quantity    INTEGER,
    image_id    BIGINT,
    sort_order  INTEGER                  NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_item_listing_entries_uuid UNIQUE (uuid),
    CONSTRAINT fk_item_listing_entries_item FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_listing_entries_image FOREIGN KEY (image_id) REFERENCES item_images (id) ON DELETE SET NULL,
    CONSTRAINT ck_item_listing_entries_quantity_min CHECK (quantity IS NULL OR quantity >= 1)
);

CREATE INDEX IF NOT EXISTS idx_item_listing_entries_item_id ON item_listing_entries (item_id);
CREATE INDEX IF NOT EXISTS idx_item_listing_entries_item_sort ON item_listing_entries (item_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_items_listing_mode ON items (listing_mode);

