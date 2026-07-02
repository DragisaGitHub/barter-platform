-- Category schema engine foundation: category_schemas, category_schema_fields, field_options.
-- Additive-only migration. Tags remain untouched; item creation is not wired to these tables yet.

-- =============================================
-- category_schemas
-- =============================================
CREATE TABLE IF NOT EXISTS category_schemas (
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID                     NOT NULL,
    category_id BIGINT                   NOT NULL,
    version     INTEGER                  NOT NULL,
    status      VARCHAR(20)              NOT NULL,
    name        VARCHAR(160)             NOT NULL,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE,
    deleted_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_category_schemas_uuid UNIQUE (uuid),
    CONSTRAINT uq_category_schemas_category_version UNIQUE (category_id, version),
    CONSTRAINT fk_category_schemas_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX IF NOT EXISTS idx_category_schemas_category_status ON category_schemas (category_id, status);

-- =============================================
-- category_schema_fields
-- =============================================
CREATE TABLE IF NOT EXISTS category_schema_fields (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID                     NOT NULL,
    schema_id       BIGINT                   NOT NULL,
    key             VARCHAR(100)             NOT NULL,
    label           VARCHAR(160)             NOT NULL,
    label_sr        VARCHAR(160),
    help_text       TEXT,
    field_type      VARCHAR(30)              NOT NULL,
    required        BOOLEAN                  NOT NULL DEFAULT FALSE,
    searchable      BOOLEAN                  NOT NULL DEFAULT FALSE,
    filterable      BOOLEAN                  NOT NULL DEFAULT FALSE,
    sortable        BOOLEAN                  NOT NULL DEFAULT FALSE,
    unit            VARCHAR(40),
    display_order   INTEGER                  NOT NULL DEFAULT 0,
    validation_json TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_category_schema_fields_uuid UNIQUE (uuid),
    CONSTRAINT uq_category_schema_fields_schema_key UNIQUE (schema_id, key),
    CONSTRAINT fk_category_schema_fields_schema FOREIGN KEY (schema_id) REFERENCES category_schemas (id)
);

CREATE INDEX IF NOT EXISTS idx_category_schema_fields_schema_order ON category_schema_fields (schema_id, display_order);

-- =============================================
-- field_options
-- =============================================
CREATE TABLE IF NOT EXISTS field_options (
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID                     NOT NULL,
    field_id      BIGINT                   NOT NULL,
    value         VARCHAR(100)             NOT NULL,
    label         VARCHAR(160)             NOT NULL,
    label_sr      VARCHAR(160),
    display_order INTEGER                  NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE,
    deleted_at    TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_field_options_uuid UNIQUE (uuid),
    CONSTRAINT uq_field_options_field_value UNIQUE (field_id, value),
    CONSTRAINT fk_field_options_field FOREIGN KEY (field_id) REFERENCES category_schema_fields (id)
);

CREATE INDEX IF NOT EXISTS idx_field_options_field_order ON field_options (field_id, display_order);

