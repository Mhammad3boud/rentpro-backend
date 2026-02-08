CREATE TABLE payments (
  id BIGSERIAL PRIMARY KEY,
  lease_id BIGINT NOT NULL REFERENCES leases(id) ON DELETE CASCADE,
  amount NUMERIC(12,2) NOT NULL,
  paid_at TIMESTAMP NOT NULL,
  method VARCHAR(30),
  reference VARCHAR(100),
  notes TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_lease_id ON payments(lease_id);
CREATE INDEX idx_payments_paid_at ON payments(paid_at);
