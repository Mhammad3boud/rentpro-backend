package com.rentpro.backend.lease.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateLeaseRequest(
        @NotNull Long unitId,
        @NotNull Long tenantId,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        BigDecimal rentAmount,
        BigDecimal depositAmount,
        String notes
) {}
