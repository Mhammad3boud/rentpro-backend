CREATE TABLE IF NOT EXISTS tenancies (
  id            BIGSERIAL PRIMARY KEY,

  unit_id       BIGINT NOT NULL,
  tenant_id     BIGINT NOT NULL,

  start_date    DATE   NOT NULL,
  end_date      DATE   NULL,

  rent_monthly  NUMERIC(12,2) NULL,
  deposit       NUMERIC(12,2) NULL,

  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT fk_tenancies_unit
    FOREIGN KEY (unit_id) REFERENCES units(id) ON DELETE CASCADE,

  CONSTRAINT fk_tenancies_tenant
    FOREIGN KEY (tenant_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_tenancies_unit_id ON tenancies(unit_id);
CREATE INDEX IF NOT EXISTS idx_tenancies_tenant_id ON tenancies(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenancies_unit_active ON tenancies(unit_id) WHERE end_date IS NULL;
