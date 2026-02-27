ALTER TABLE maintenance_requests
ALTER COLUMN lease_id DROP NOT NULL;

ALTER TABLE maintenance_requests
ALTER COLUMN tenant_id DROP NOT NULL;
