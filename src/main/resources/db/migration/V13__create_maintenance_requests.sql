CREATE TABLE maintenance_requests (
  id BIGSERIAL PRIMARY KEY,

  unit_id BIGINT NOT NULL REFERENCES units(id) ON DELETE CASCADE,
  tenant_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  title VARCHAR(120) NOT NULL,
  description TEXT,
  priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- LOW/MEDIUM/HIGH
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',     -- OPEN/IN_PROGRESS/RESOLVED

  owner_notes TEXT,

  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  resolved_at TIMESTAMP
);

CREATE INDEX idx_mr_unit_id ON maintenance_requests(unit_id);
CREATE INDEX idx_mr_tenant_id ON maintenance_requests(tenant_id);
CREATE INDEX idx_mr_status ON maintenance_requests(status);
CREATE INDEX idx_mr_created_at ON maintenance_requests(created_at);
