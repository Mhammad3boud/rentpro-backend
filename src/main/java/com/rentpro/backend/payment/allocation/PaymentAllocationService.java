package com.rentpro.backend.payment.allocation;

import com.rentpro.backend.invoice.Invoice;
import com.rentpro.backend.invoice.InvoiceRepository;
import com.rentpro.backend.payment.Payment;
import com.rentpro.backend.payment.PaymentRepository;
import com.rentpro.backend.payment.allocation.dto.AllocatePaymentRequest;
import com.rentpro.backend.unit.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentAllocationService {

    private final PaymentRepository paymentRepo;
    private final InvoiceRepository invoiceRepo;
    private final PaymentAllocationRepository allocRepo;
    private final UnitRepository unitRepo;

    public PaymentAllocationService(
            PaymentRepository paymentRepo,
            InvoiceRepository invoiceRepo,
            PaymentAllocationRepository allocRepo,
            UnitRepository unitRepo) {
        this.paymentRepo = paymentRepo;
        this.invoiceRepo = invoiceRepo;
        this.allocRepo = allocRepo;
        this.unitRepo = unitRepo;
    }

    @Transactional
    public List<PaymentAllocation> allocate(Long ownerId, Long paymentId, AllocatePaymentRequest req) {

        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // Owner check: payment’s lease unit must belong to owner
        unitRepo.findByIdAndProperty_Owner_Id(payment.getLease().getUnit().getId(), ownerId)
                .orElseThrow(() -> new RuntimeException("Payment not found or not yours"));

        // Total allocations must be <= payment.amount
        BigDecimal sumAlloc = req.allocations().stream()
                .map(AllocatePaymentRequest.Item::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumAlloc.compareTo(payment.getAmount()) > 0) {
            throw new RuntimeException("Allocated total exceeds payment amount");
        }

        // Create allocations
        return req.allocations().stream().map(item -> {
            Invoice inv = invoiceRepo.findById(item.invoiceId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));

            // Same lease guard (important!)
            if (!inv.getLease().getId().equals(payment.getLease().getId())) {
                throw new RuntimeException("Invoice does not belong to the same lease");
            }

            PaymentAllocation pa = PaymentAllocation.builder()
                    .payment(payment)
                    .invoice(inv)
                    .amountApplied(item.amount())
                    .build();

            return allocRepo.save(pa);
        }).toList();
    }
}
