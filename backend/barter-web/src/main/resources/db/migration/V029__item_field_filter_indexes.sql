-- Marketplace Schema Engine, Phase 6: supplementary indexes to keep dynamic category filter
-- queries performant. These complement the existing idx_item_field_values_item/field indexes
-- from V028 by covering the typed value columns and the option join table used when filtering.
-- Additive-only migration.

CREATE INDEX IF NOT EXISTS idx_item_field_values_field_option
    ON item_field_values (schema_field_id, option_id);

CREATE INDEX IF NOT EXISTS idx_item_field_values_field_number
    ON item_field_values (schema_field_id, value_number);

CREATE INDEX IF NOT EXISTS idx_item_field_values_field_boolean
    ON item_field_values (schema_field_id, value_boolean);

CREATE INDEX IF NOT EXISTS idx_item_field_values_field_date
    ON item_field_values (schema_field_id, value_date);

CREATE INDEX IF NOT EXISTS idx_item_field_value_options_option
    ON item_field_value_options (field_option_id);

