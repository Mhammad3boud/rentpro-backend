package com.rentpro.backend.payment.dto;

import com.rentpro.backend.payment.gateway.CheckoutStatus;
import com.rentpro.backend.payment.gateway.PaymentProvider;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CheckoutStatusResponse(
        PaymentProvider provider,
        String sessionId,
        UUID paymentId,
        CheckoutStatus status,
        String message,
        BigDecimal amountPaid,
        String currency,
        Instant updatedAt
) {}
