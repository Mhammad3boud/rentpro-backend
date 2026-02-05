CREATE TABLE properties (
  id BIGSERIAL PRIMARY KEY,
  owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  title VARCHAR(120) NOT NULL,
  address VARCHAR(255),

  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,

  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_properties_owner_id ON properties(owner_id);
CREATE INDEX idx_properties_lat_lng ON properties(latitude, longitude);
