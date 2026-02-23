package com.rentpro.backend.payment.dto;

import com.rentpro.backend.payment.RentPayment.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateOrUpdatePaymentRequest(
        UUID leaseId,
        String monthYear,          // "2026-02"
        BigDecimal amountExpected, // monthly rent
        BigDecimal amountPaid,     // amount received so far
        LocalDate dueDate,
        LocalDate paidDate,
        PaymentMethod paymentMethod
) {}
