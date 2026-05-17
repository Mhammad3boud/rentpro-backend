package com.rentpro.backend.payment.gateway;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentCheckoutSessionRepository extends JpaRepository<PaymentCheckoutSession, UUID> {
    Optional<PaymentCheckoutSession> findByProviderAndProviderSessionId(PaymentProvider provider, String providerSessionId);
    Optional<PaymentCheckoutSession> findByProviderAndProviderReference(PaymentProvider provider, String providerReference);
}
