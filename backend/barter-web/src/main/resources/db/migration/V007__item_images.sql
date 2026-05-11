-- Item images table
-- =============================================
-- item_images
-- =============================================
CREATE TABLE IF NOT EXISTS item_images (
    id                BIGSERIAL                NOT NULL,
    uuid              UUID                     NOT NULL,
    item_id           BIGINT                   NOT NULL,
    storage_key       VARCHAR(500)             NOT NULL,
    original_filename VARCHAR(255)             NOT NULL,
    content_type      VARCHAR(100)             NOT NULL,
    file_size         BIGINT                   NOT NULL,
    sort_order        INTEGER                  NOT NULL DEFAULT 0,
    is_primary        BOOLEAN                  NOT NULL DEFAULT false,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_item_images PRIMARY KEY (id),
    CONSTRAINT uq_item_images_uuid UNIQUE (uuid),
    CONSTRAINT fk_item_images_item FOREIGN KEY (item_id) REFERENCES items (id)
);

CREATE INDEX IF NOT EXISTS idx_item_images_item_id
    ON item_images (item_id);

CREATE INDEX IF NOT EXISTS idx_item_images_item_id_sort_order
    ON item_images (item_id, sort_order);

-- Only one primary image per item
CREATE UNIQUE INDEX IF NOT EXISTS uq_item_images_primary_per_item
    ON item_images (item_id)
    WHERE is_primary = true;

