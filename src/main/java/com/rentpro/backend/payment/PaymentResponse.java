package com.rentpro.backend.payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long leaseId,
        BigDecimal amount,
        Instant paidAt,
        String method,
        String reference,
        String notes
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getLease().getId(),
                p.getAmount(),
                p.getPaidAt(),
                p.getMethod(),
                p.getReference(),
                p.getNotes()
        );
    }
}
