package com.rentpro.backend.payment.dto;

import java.math.BigDecimal;

public record MonthlyPaymentStatusItem(
        String month,                 // "2026-02"
        BigDecimal expectedRent,
        BigDecimal paidTotal,
        String status,                // "PAID" | "PARTIAL" | "UNPAID" | "OVERDUE" | "N/A"
        boolean inLeasePeriod
) {}
