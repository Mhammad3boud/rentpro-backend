package com.rentpro.backend.lease.dto;

import com.rentpro.backend.lease.DepositBreakdownItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AssignTenantRequest(
        UUID tenantId,
        UUID propertyId,
        UUID unitId,      // nullable for standalone properties
        BigDecimal monthlyRent,
        BigDecimal securityDeposit,
        List<DepositBreakdownItem> depositBreakdown,
        LocalDate startDate,
        LocalDate endDate  // nullable for open-ended leases
) {}
