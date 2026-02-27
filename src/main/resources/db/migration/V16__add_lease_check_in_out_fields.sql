ALTER TABLE leases
    ADD COLUMN IF NOT EXISTS check_in_date DATE,
    ADD COLUMN IF NOT EXISTS checked_in_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS check_in_notes TEXT,
    ADD COLUMN IF NOT EXISTS check_out_date DATE,
    ADD COLUMN IF NOT EXISTS checked_out_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS check_out_reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS check_out_notes TEXT;
