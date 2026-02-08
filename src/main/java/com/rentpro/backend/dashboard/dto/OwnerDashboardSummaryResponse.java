package com.rentpro.backend.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record OwnerDashboardSummaryResponse(
        String from, // "yyyy-MM"
        String to, // "yyyy-MM"
        BigDecimal totalExpected, // sum invoice.amount
        BigDecimal totalPaid, // sum allocations
        BigDecimal outstanding, // expected - paid
        BigDecimal collectionRate, // paid/expected (0..1)
        List<MonthlyCollectionItem> months) {
    public record MonthlyCollectionItem(
            String month, // "yyyy-MM"
            BigDecimal expected,
            BigDecimal paid,
            BigDecimal outstanding,
            BigDecimal collectionRate) {
    }
}
