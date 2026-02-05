CREATE TABLE units (
  id BIGSERIAL PRIMARY KEY,

  property_id BIGINT NOT NULL REFERENCES properties(id) ON DELETE CASCADE,

  unit_no VARCHAR(50) NOT NULL,          -- e.g. A-01, Room 2, Unit 3B
  floor VARCHAR(20),                     -- optional
  type VARCHAR(30),                      -- optional: ROOM, STUDIO, 2BR, etc.
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_units_property_id ON units(property_id);

-- prevent duplicate unit numbers within the same property
CREATE UNIQUE INDEX ux_units_property_unitno ON units(property_id, unit_no);
