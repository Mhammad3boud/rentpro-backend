package com.rentpro.backend.payment.stripe;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StripeIntentRequest(
        @NotNull UUID leaseId,
        @NotBlank String monthYear,
        @NotNull @DecimalMin("0.01") BigDecimal amountExpected,
        @NotNull @DecimalMin("0.01") BigDecimal amountPaid,
        LocalDate paidDate,
        String paymentMethod
) {}
