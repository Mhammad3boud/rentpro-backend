package com.rentpro.backend.lease.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AssignTenantRequest(
        UUID tenantId,
        UUID propertyId,
        UUID unitId,      // nullable for standalone properties
        BigDecimal monthlyRent,
        LocalDate startDate,
        LocalDate endDate  // nullable for open-ended leases
) {}
