package com.rentpro.backend.payment.allocation.dto;

import com.rentpro.backend.payment.allocation.PaymentAllocation;

import java.math.BigDecimal;

public record PaymentAllocationResponse(
        Long id,
        Long paymentId,
        Long invoiceId,
        BigDecimal amountApplied
) {
    public static PaymentAllocationResponse from(PaymentAllocation pa) {
        return new PaymentAllocationResponse(
                pa.getId(),
                pa.getPayment().getId(),
                pa.getInvoice().getId(),
                pa.getAmountApplied()
        );
    }
}
