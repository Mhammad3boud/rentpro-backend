package com.rentpro.backend.payment.allocation;

import com.rentpro.backend.invoice.Invoice;
import com.rentpro.backend.payment.Payment;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_allocations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"payment_id", "invoice_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PaymentAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "amount_applied", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountApplied;
}
