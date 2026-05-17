package com.rentpro.backend.payment.dto;

import com.rentpro.backend.payment.gateway.PaymentProvider;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateCheckoutSessionRequest(
        @NotNull UUID leaseId,
        @NotBlank String monthYear,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amountExpected,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amountPaid,
        LocalDate paidDate,
        @NotNull PaymentProvider provider,
        @NotBlank String returnUrl,
        @NotBlank String cancelUrl
) {}
