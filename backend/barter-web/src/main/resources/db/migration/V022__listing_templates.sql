ALTER TABLE items
    ADD COLUMN IF NOT EXISTS listing_template_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS template_metadata_json TEXT;

UPDATE items
SET listing_template_type = CASE listing_mode
    WHEN 'BUNDLE' THEN 'BUNDLE'
    WHEN 'PICK_ANY' THEN 'PICK_FROM_COLLECTION'
    ELSE 'STANDARD_ITEM'
END
WHERE listing_template_type IS NULL;

CREATE INDEX IF NOT EXISTS idx_items_listing_template_type ON items (listing_template_type);

