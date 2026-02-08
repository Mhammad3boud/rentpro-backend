CREATE TABLE invoices (
  id BIGSERIAL PRIMARY KEY,
  lease_id BIGINT NOT NULL REFERENCES leases(id) ON DELETE CASCADE,

  period_year INT NOT NULL,
  period_month INT NOT NULL, -- 1..12

  amount NUMERIC(12,2) NOT NULL,
  due_date DATE NOT NULL,

  status VARCHAR(20) NOT NULL DEFAULT 'UNPAID', -- UNPAID / PAID / OVERDUE
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Prevent duplicates: 1 invoice per lease per month
CREATE UNIQUE INDEX uq_invoice_lease_period
ON invoices (lease_id, period_year, period_month);

CREATE INDEX idx_invoice_lease_id ON invoices (lease_id);
CREATE INDEX idx_invoice_due_date ON invoices (due_date);
