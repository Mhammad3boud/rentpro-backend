package com.rentpro.backend.lease.dto;

import com.rentpro.backend.lease.DepositBreakdownItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateLeaseRequest(
        BigDecimal monthlyRent,
        BigDecimal securityDeposit,
        List<DepositBreakdownItem> depositBreakdown,
        LocalDate startDate,
        LocalDate endDate
) {}
