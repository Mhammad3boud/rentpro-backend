package com.rentpro.backend.dashboard.dto;

import java.math.BigDecimal;

public record CurrencyBreakdown(
        String currency,
        BigDecimal monthlyRevenue,
        BigDecimal totalOutstanding
) {}
