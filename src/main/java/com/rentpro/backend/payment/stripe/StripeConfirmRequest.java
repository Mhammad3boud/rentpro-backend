package com.rentpro.backend.payment.stripe;

import jakarta.validation.constraints.NotBlank;

public record StripeConfirmRequest(
        @NotBlank String paymentIntentId
) {}
