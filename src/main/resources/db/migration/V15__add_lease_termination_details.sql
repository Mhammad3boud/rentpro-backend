ALTER TABLE leases
    ADD COLUMN IF NOT EXISTS termination_reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS termination_notes TEXT,
    ADD COLUMN IF NOT EXISTS termination_date DATE,
    ADD COLUMN IF NOT EXISTS terminated_at TIMESTAMP;
