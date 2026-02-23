package com.rentpro.backend.lease.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateLeaseRequest(
        BigDecimal monthlyRent,
        BigDecimal securityDeposit,
        LocalDate startDate,
        LocalDate endDate
) {}
