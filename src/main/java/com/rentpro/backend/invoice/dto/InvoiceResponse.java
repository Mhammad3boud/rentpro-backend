package com.rentpro.backend.invoice.dto;

import com.rentpro.backend.invoice.Invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceResponse(
        Long id,
        Long leaseId,
        String period,      // "yyyy-MM"
        BigDecimal amount,
        LocalDate dueDate,
        String status
) {
    public static InvoiceResponse from(Invoice i) {
        String period = String.format("%04d-%02d", i.getPeriodYear(), i.getPeriodMonth());
        return new InvoiceResponse(i.getId(), i.getLease().getId(), period, i.getAmount(), i.getDueDate(), i.getStatus());
    }
}
