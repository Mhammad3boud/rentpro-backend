package com.rentpro.backend.lease.dto;

import com.rentpro.backend.lease.DepositBreakdownItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RenewLeaseRequest(
        LocalDate newStartDate,
        LocalDate newEndDate,
        BigDecimal monthlyRent,
        BigDecimal securityDeposit,
        List<DepositBreakdownItem> depositBreakdown,
        UUID templateId
) {}
