-- Privacy-safe approximate exchange location fields for item listings.
-- These fields intentionally store coarse free-text city/area/location labels only;
-- no exact addresses, GPS coordinates, geocoding metadata, or distance data are stored.

ALTER TABLE items
    ADD COLUMN exchange_location VARCHAR(255),
    ADD COLUMN exchange_city VARCHAR(120),
    ADD COLUMN exchange_area VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_items_exchange_city_lower ON items (LOWER(exchange_city));
CREATE INDEX IF NOT EXISTS idx_items_exchange_area_lower ON items (LOWER(exchange_area));
CREATE INDEX IF NOT EXISTS idx_items_exchange_location_lower ON items (LOWER(exchange_location));

