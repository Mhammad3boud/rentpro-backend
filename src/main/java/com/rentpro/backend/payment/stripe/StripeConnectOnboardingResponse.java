package com.rentpro.backend.payment.stripe;

public record StripeConnectOnboardingResponse(
        String onboardingUrl,
        String stripeAccountId
) {}
