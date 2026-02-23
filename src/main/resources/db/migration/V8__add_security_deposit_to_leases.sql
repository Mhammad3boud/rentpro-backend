-- Add security_deposit column to leases table (idempotent)
ALTER TABLE leases ADD COLUMN IF NOT EXISTS security_deposit DECIMAL(10, 2);

-- Set default value based on monthly_rent for existing records
UPDATE leases SET security_deposit = monthly_rent * 2 WHERE security_deposit IS NULL;
