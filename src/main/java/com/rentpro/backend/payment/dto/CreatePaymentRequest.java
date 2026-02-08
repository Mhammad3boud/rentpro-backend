package com.rentpro.backend.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public record CreatePaymentRequest(
        @NotNull Long leaseId,
        @NotNull @Positive BigDecimal amount,
        @NotNull Instant paidAt,
        String method,
        String reference,
        String notes
) {}
