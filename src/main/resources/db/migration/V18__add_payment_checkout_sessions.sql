CREATE TABLE payment_checkout_sessions (
    checkout_session_id UUID PRIMARY KEY,
    lease_id UUID NOT NULL REFERENCES leases(lease_id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL,
    month_year VARCHAR(7) NOT NULL,
    amount_expected NUMERIC(10,2) NOT NULL,
    amount_paid NUMERIC(10,2) NOT NULL,
    paid_date DATE NULL,
    return_url VARCHAR(1024) NOT NULL,
    cancel_url VARCHAR(1024) NOT NULL,
    provider_session_id VARCHAR(255) NULL,
    provider_reference VARCHAR(255) NULL,
    checkout_url VARCHAR(2048) NULL,
    status VARCHAR(20) NOT NULL,
    message VARCHAR(1024) NULL,
    last_provider_payload TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_checkout_provider_session
    ON payment_checkout_sessions(provider, provider_session_id);

CREATE INDEX idx_payment_checkout_provider_reference
    ON payment_checkout_sessions(provider, provider_reference);
