-- Drop the old check constraint that only allowed CASH, BANK_TRANSFER, CHECK
ALTER TABLE rent_payments DROP CONSTRAINT IF EXISTS rent_payments_payment_method_check;

-- Re-add with all supported payment methods including gateway and local wallet methods
ALTER TABLE rent_payments
    ADD CONSTRAINT rent_payments_payment_method_check
    CHECK (payment_method IN (
        'CASH', 'BANK_TRANSFER', 'CHECK',
        'PAYPAL', 'ADYEN', 'STRIPE',
        'FPX', 'DUITNOW', 'TOUCHNGO', 'GRABPAY',
        'MPESA', 'AIRTEL_MONEY', 'TIGO_PESA'
    ));
