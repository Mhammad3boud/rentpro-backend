ALTER TABLE properties
ADD COLUMN category VARCHAR(30) NOT NULL DEFAULT 'RENTAL',
ADD COLUMN notes TEXT,
ADD COLUMN meta JSONB;

CREATE INDEX IF NOT EXISTS idx_properties_category ON properties(category);
