-- A "lease" (occupancy record) keeps tenant history per unit.
-- One unit can have many leases over time, but only ONE active lease at a time.

CREATE TABLE IF NOT EXISTS leases (
    id BIGSERIAL PRIMARY KEY,

    unit_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,

    start_date DATE NOT NULL,
    end_date DATE NULL,

    rent_amount NUMERIC(12,2) NULL,
    deposit_amount NUMERIC(12,2) NULL,
    notes TEXT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_leases_unit
        FOREIGN KEY (unit_id) REFERENCES units(id) ON DELETE CASCADE,

    CONSTRAINT fk_leases_tenant
        FOREIGN KEY (tenant_id) REFERENCES users(id) ON DELETE RESTRICT,

    CONSTRAINT chk_lease_dates
        CHECK (end_date IS NULL OR end_date >= start_date)
);

-- Ensure only ONE active lease (end_date is null) per unit
CREATE UNIQUE INDEX IF NOT EXISTS ux_leases_unit_active
ON leases(unit_id)
WHERE end_date IS NULL;

-- Helpful indexes for history lookups
CREATE INDEX IF NOT EXISTS ix_leases_unit_id ON leases(unit_id);
CREATE INDEX IF NOT EXISTS ix_leases_tenant_id ON leases(tenant_id);
