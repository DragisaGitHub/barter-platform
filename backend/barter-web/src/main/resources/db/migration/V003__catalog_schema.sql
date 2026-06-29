-- Catalog domain schema: categories, tags, items, item_tags

-- =============================================
-- categories
-- =============================================
CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID                     NOT NULL,
    name        VARCHAR(120)             NOT NULL,
    slug        VARCHAR(140)             NOT NULL,
    description TEXT,
    parent_id   BIGINT,
    sort_order  INTEGER                  NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE,
    deleted_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_categories_uuid UNIQUE (uuid),
    CONSTRAINT uq_categories_slug UNIQUE (slug),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id)
);

CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories (parent_id);

-- =============================================
-- tags
-- =============================================
CREATE TABLE IF NOT EXISTS tags (
    id         BIGSERIAL PRIMARY KEY,
    uuid       UUID                     NOT NULL,
    name       VARCHAR(120)             NOT NULL,
    slug       VARCHAR(140)             NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_tags_uuid UNIQUE (uuid),
    CONSTRAINT uq_tags_slug UNIQUE (slug)
);

-- =============================================
-- items
-- =============================================
CREATE TABLE IF NOT EXISTS items (
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID                     NOT NULL,
    owner_id    BIGINT                   NOT NULL,
    category_id BIGINT                   NOT NULL,
    title       VARCHAR(200)             NOT NULL,
    description TEXT,
    status      VARCHAR(40)              NOT NULL,
    condition   VARCHAR(40)              NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    removed_at  TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE,
    deleted_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_items_uuid UNIQUE (uuid),
    CONSTRAINT fk_items_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX IF NOT EXISTS idx_items_owner_id ON items (owner_id);
CREATE INDEX IF NOT EXISTS idx_items_category_id ON items (category_id);
CREATE INDEX IF NOT EXISTS idx_items_status ON items (status);
CREATE INDEX IF NOT EXISTS idx_items_condition ON items (condition);
CREATE INDEX IF NOT EXISTS idx_items_status_created_at ON items (status, created_at);

-- =============================================
-- item_tags
-- =============================================
CREATE TABLE IF NOT EXISTS item_tags (
    item_id     BIGINT                   NOT NULL,
    tag_id      BIGINT                   NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_item_tags PRIMARY KEY (item_id, tag_id),
    CONSTRAINT fk_item_tags_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT fk_item_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id)
);

CREATE INDEX IF NOT EXISTS idx_item_tags_item_id ON item_tags (item_id);
CREATE INDEX IF NOT EXISTS idx_item_tags_tag_id ON item_tags (tag_id);

-- =============================================
-- NOTE: Default categories and tags are NOT seeded via Flyway.
-- They are inserted by DemoContentSeeder (Java) only when
-- barter.seed.demo-content=true (disabled by default).
-- Production starts with an empty catalog; admins create
-- categories and tags manually after launch.
-- =============================================

