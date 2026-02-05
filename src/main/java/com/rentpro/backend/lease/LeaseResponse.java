package com.rentpro.backend.lease;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LeaseResponse(
        Long id,
        Long unitId,
        Long tenantId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal rentAmount,
        BigDecimal depositAmount,
        String notes,
        boolean active,
        Instant createdAt
) {
    public static LeaseResponse from(Lease lease) {
        return new LeaseResponse(
                lease.getId(),
                lease.getUnit().getId(),
                lease.getTenant().getId(),
                lease.getStartDate(),
                lease.getEndDate(),
                lease.getRentAmount(),
                lease.getDepositAmount(),
                lease.getNotes(),
                lease.getEndDate() == null,
                lease.getCreatedAt()
        );
    }
}
