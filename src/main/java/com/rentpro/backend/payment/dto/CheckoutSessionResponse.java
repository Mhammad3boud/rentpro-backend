package com.rentpro.backend.payment.dto;

import com.rentpro.backend.payment.gateway.PaymentProvider;

import java.util.UUID;

public record CheckoutSessionResponse(
        String checkoutUrl,
        PaymentProvider provider,
        String sessionId,
        UUID paymentId,
        String clientKey
) {}
