-- 1) Add structure fields to properties
ALTER TABLE properties
ADD COLUMN IF NOT EXISTS structure_type VARCHAR(20) NOT NULL DEFAULT 'STANDALONE',
ADD COLUMN IF NOT EXISTS unit_count INT;

CREATE INDEX IF NOT EXISTS idx_properties_structure_type ON properties(structure_type);

-- 2) Units table (rooms / units inside a property)
CREATE TABLE IF NOT EXISTS units (
  id BIGSERIAL PRIMARY KEY,
  property_id BIGINT NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
  unit_no VARCHAR(50) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_units_property_id ON units(property_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_units_property_unitno ON units(property_id, unit_no);
