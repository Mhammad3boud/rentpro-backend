package com.rentpro.backend.payment;

public enum PaymentMethod {
    // Manual methods
    CASH,
    BANK_TRANSFER,
    CHECK,
    // Gateway-routed
    PAYPAL,
    ADYEN,
    STRIPE,
    // Malaysia — via Adyen Drop-in
    FPX,
    DUITNOW,
    TOUCHNGO,
    GRABPAY,
    // Tanzania — via Adyen Drop-in
    MPESA,
    AIRTEL_MONEY,
    TIGO_PESA
}
