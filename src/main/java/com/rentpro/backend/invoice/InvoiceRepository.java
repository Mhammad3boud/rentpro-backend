package com.rentpro.backend.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findAllByLease_IdOrderByPeriodYearDescPeriodMonthDesc(Long leaseId);

    Optional<Invoice> findByLease_IdAndPeriodYearAndPeriodMonth(Long leaseId, int year, int month);

    List<Invoice> findAllByLease_Unit_Property_Owner_Id(Long ownerId);

}
