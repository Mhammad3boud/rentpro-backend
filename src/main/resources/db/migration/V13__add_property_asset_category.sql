ALTER TABLE properties
ADD COLUMN IF NOT EXISTS asset_category VARCHAR(20);

UPDATE properties
SET asset_category = CASE
    WHEN property_type = 'MULTI_UNIT' THEN 'APARTMENT'
    ELSE 'HOUSE'
END
WHERE asset_category IS NULL;

ALTER TABLE properties
ALTER COLUMN asset_category SET DEFAULT 'OTHER';

ALTER TABLE properties
ADD CONSTRAINT chk_properties_asset_category
CHECK (asset_category IN ('HOUSE','APARTMENT','LAND','FARM','SHOP','OFFICE','WAREHOUSE','OTHER'));
