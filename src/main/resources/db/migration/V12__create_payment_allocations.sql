CREATE TABLE payment_allocations (
  id BIGSERIAL PRIMARY KEY,
  payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
  invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
  amount_applied NUMERIC(12,2) NOT NULL
);

-- Prevent duplicate “same payment applied twice to same invoice”
CREATE UNIQUE INDEX uq_payment_alloc_payment_invoice
ON payment_allocations (payment_id, invoice_id);

CREATE INDEX idx_alloc_payment_id ON payment_allocations (payment_id);
CREATE INDEX idx_alloc_invoice_id ON payment_allocations (invoice_id);
