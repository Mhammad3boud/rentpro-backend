package com.rentpro.backend.lease.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateLeaseRequest(
        UUID propertyId,
        UUID unitId,      // nullable for standalone
        UUID tenantId,    // Tenant table ID
        BigDecimal monthlyRent,
        LocalDate startDate,
        LocalDate endDate
) {}
