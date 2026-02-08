package com.rentpro.backend.invoice.dto;

import jakarta.validation.constraints.NotNull;

public record GenerateInvoicesRequest(
        @NotNull Long leaseId,
        @NotNull String from, // "yyyy-MM"
        @NotNull String to,   // "yyyy-MM"
        Integer dueDay        // optional, default 5 (means due on 5th)
) {}
