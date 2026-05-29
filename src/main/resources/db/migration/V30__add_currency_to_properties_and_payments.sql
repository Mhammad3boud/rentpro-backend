-- Add currency to properties (owner sets this when creating a property)
ALTER TABLE properties ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'MYR';

-- Add currency to rent_payments (copied from property at payment creation time for historical accuracy)
ALTER TABLE rent_payments ADD COLUMN currency VARCHAR(3);

-- Back-fill existing payments from their property via the lease
UPDATE rent_payments rp
SET currency = p.currency
FROM leases l
JOIN properties p ON p.property_id = l.property_id
WHERE rp.lease_id = l.lease_id;
