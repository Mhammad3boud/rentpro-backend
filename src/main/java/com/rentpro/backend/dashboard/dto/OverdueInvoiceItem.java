package com.rentpro.backend.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OverdueInvoiceItem(
        Long invoiceId,
        Long leaseId,
        String period, // "yyyy-MM"
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal paidTotal,
        BigDecimal remaining,
        String status // OVERDUE or PARTIAL
) {
}
