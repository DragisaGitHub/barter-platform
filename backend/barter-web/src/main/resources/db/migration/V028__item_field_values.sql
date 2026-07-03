-- Item field values: persists dynamic category-schema-driven field values entered on items.
-- Additive-only migration. Values are always fully replaced (delete + insert) by the owning
-- application service, so no soft-delete columns are required here.

-- =============================================
-- item_field_values
-- =============================================
CREATE TABLE IF NOT EXISTS item_field_values (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID                     NOT NULL,
    item_id         BIGINT                   NOT NULL,
    schema_field_id BIGINT                   NOT NULL,
    value_text      TEXT,
    value_number    NUMERIC(20, 6),
    value_boolean   BOOLEAN,
    value_date      DATE,
    option_id       BIGINT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_item_field_values_uuid UNIQUE (uuid),
    CONSTRAINT uq_item_field_values_item_field UNIQUE (item_id, schema_field_id),
    CONSTRAINT fk_item_field_values_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT fk_item_field_values_field FOREIGN KEY (schema_field_id) REFERENCES category_schema_fields (id),
    CONSTRAINT fk_item_field_values_option FOREIGN KEY (option_id) REFERENCES field_options (id)
);

CREATE INDEX IF NOT EXISTS idx_item_field_values_item ON item_field_values (item_id);
CREATE INDEX IF NOT EXISTS idx_item_field_values_field ON item_field_values (schema_field_id);

-- =============================================
-- item_field_value_options (MULTI_SELECT selections)
-- =============================================
CREATE TABLE IF NOT EXISTS item_field_value_options (
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID                     NOT NULL,
    item_field_value_id BIGINT                   NOT NULL,
    field_option_id     BIGINT                   NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_item_field_value_options_uuid UNIQUE (uuid),
    CONSTRAINT uq_item_field_value_options_value_option UNIQUE (item_field_value_id, field_option_id),
    CONSTRAINT fk_item_field_value_options_value FOREIGN KEY (item_field_value_id) REFERENCES item_field_values (id),
    CONSTRAINT fk_item_field_value_options_option FOREIGN KEY (field_option_id) REFERENCES field_options (id)
);

CREATE INDEX IF NOT EXISTS idx_item_field_value_options_value ON item_field_value_options (item_field_value_id);

