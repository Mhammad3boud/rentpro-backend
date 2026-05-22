package com.rentpro.backend.lease.dto;

import com.rentpro.backend.lease.DepositBreakdownItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateLeaseRequest(
        UUID propertyId,
        UUID unitId,      // nullable for standalone
        UUID tenantId,    // Tenant table ID
        BigDecimal monthlyRent,
        BigDecimal securityDeposit,
        List<DepositBreakdownItem> depositBreakdown,
        LocalDate startDate,
        LocalDate endDate
) {}
