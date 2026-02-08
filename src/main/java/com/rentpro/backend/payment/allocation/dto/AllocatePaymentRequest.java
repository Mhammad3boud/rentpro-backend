package com.rentpro.backend.payment.allocation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record AllocatePaymentRequest(
        @NotEmpty List<Item> allocations
) {
    public record Item(
            @NotNull Long invoiceId,
            @NotNull @Positive BigDecimal amount
    ) {}
}
