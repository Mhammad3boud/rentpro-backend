package com.rentpro.backend.payment.stripe;

public record StripeIntentResponse(
        String clientSecret,
        String publishableKey,
        String paymentIntentId,
        String currency
) {}
