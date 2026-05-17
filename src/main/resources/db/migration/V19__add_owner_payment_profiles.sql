CREATE TABLE owner_payment_profiles (
    profile_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    country           VARCHAR(2)   NOT NULL DEFAULT 'MY',
    accepted_methods  TEXT         NOT NULL DEFAULT '',
    bank_name         VARCHAR(100),
    bank_account_number VARCHAR(50),
    bank_account_name VARCHAR(100),
    bank_swift_code   VARCHAR(20),
    duitnow_id        VARCHAR(50),
    touchngo_phone    VARCHAR(20),
    grabpay_phone     VARCHAR(20),
    mpesa_phone       VARCHAR(20),
    airtel_money_phone VARCHAR(20),
    tigo_pesa_phone   VARCHAR(20),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_owner_payment_profiles_user ON owner_payment_profiles(user_id);
