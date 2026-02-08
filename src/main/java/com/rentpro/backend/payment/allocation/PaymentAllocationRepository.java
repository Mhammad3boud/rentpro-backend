package com.rentpro.backend.payment.allocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {
    List<PaymentAllocation> findAllByInvoice_Id(Long invoiceId);

    List<PaymentAllocation> findAllByPayment_Id(Long paymentId);

    @Query("""
                SELECT COALESCE(SUM(pa.amountApplied), 0)
                FROM PaymentAllocation pa
                WHERE pa.invoice.id = :invoiceId
            """)
    BigDecimal sumAppliedByInvoiceId(@Param("invoiceId") Long invoiceId);
}
