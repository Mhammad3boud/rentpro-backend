package com.rentpro.backend.payment.dto;

import java.math.BigDecimal;
import java.util.List;

public record LeasePaymentStatusResponse(
        Long leaseId,
        BigDecimal expectedRent,
        List<MonthlyPaymentStatusItem> months,

        BigDecimal totalExpected,
        BigDecimal totalPaid,
        BigDecimal outstanding,

        int paidMonthsCount,
        int partialMonthsCount,
        int unpaidMonthsCount
) {}
