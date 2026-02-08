package com.rentpro.backend.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.Instant;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllByLease_IdOrderByPaidAtDesc(Long leaseId);

    List<Payment> findAllByLease_IdAndPaidAtBetweenOrderByPaidAtAsc(
            Long leaseId, Instant from, Instant to);
}
