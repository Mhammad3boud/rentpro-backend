package com.rentpro.backend.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TenantDashboardResponse(
        BigDecimal totalOutstanding,
        long overdueCount,
        LocalDate nextDueDate,
        long maintenanceCount
) {}
