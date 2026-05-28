ALTER TABLE contract_templates
    ADD COLUMN IF NOT EXISTS template_type VARCHAR(50) DEFAULT 'INITIAL';

ALTER TABLE leases
    ADD COLUMN IF NOT EXISTS previous_lease_id UUID REFERENCES leases(lease_id) ON DELETE SET NULL;
