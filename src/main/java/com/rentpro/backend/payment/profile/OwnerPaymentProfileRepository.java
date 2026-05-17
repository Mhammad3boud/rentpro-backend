package com.rentpro.backend.payment.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OwnerPaymentProfileRepository extends JpaRepository<OwnerPaymentProfile, UUID> {
    Optional<OwnerPaymentProfile> findByUser_UserId(UUID userId);
}
