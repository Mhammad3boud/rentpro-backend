ALTER TABLE owner_payment_profiles
    ADD COLUMN stripe_account_id  VARCHAR(255),
    ADD COLUMN stripe_onboarded   BOOLEAN NOT NULL DEFAULT FALSE;
