package com.rentpro.backend.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record OwnerDashboardResponse(
        BigDecimal totalExpected,
        BigDecimal totalCollected,
        BigDecimal totalOutstanding,
        BigDecimal monthlyRevenue,
        long overdueCount,
        long partialCount,
        long pendingCount,
        long maintenancePending,
        long maintenanceInProgress,
        long maintenanceResolved,
        long maintenanceRejected,
        long totalProperties,
        long totalTenants,
        long activeLeases,
        List<CurrencyBreakdown> revenueBreakdown
) {}
