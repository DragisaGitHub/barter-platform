-- Favorites / wishlist foundation for catalog items

CREATE TABLE IF NOT EXISTS favorite_items (
    id         BIGSERIAL PRIMARY KEY,
    uuid       UUID                     NOT NULL,
    user_id    BIGINT                   NOT NULL,
    item_id    BIGINT                   NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_favorite_items_uuid UNIQUE (uuid),
    CONSTRAINT uq_favorite_items_user_item UNIQUE (user_id, item_id),
    CONSTRAINT fk_favorite_items_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_items_item FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_favorite_items_user_id ON favorite_items (user_id);
CREATE INDEX IF NOT EXISTS idx_favorite_items_item_id ON favorite_items (item_id);

