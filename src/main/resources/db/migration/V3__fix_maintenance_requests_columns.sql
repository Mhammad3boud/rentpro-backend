-- Add missing columns expected by the JPA entity

ALTER TABLE maintenance_requests
  ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE maintenance_requests
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NULL;

ALTER TABLE maintenance_requests
  ADD COLUMN IF NOT EXISTS owner_comment TEXT NULL;

-- Optional: helpful index for owner filtering + status
CREATE INDEX IF NOT EXISTS idx_maintenance_requests_property_status
  ON maintenance_requests(property_id, status);
