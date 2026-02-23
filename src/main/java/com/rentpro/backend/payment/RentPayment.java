package com.rentpro.backend.payment;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.rentpro.backend.lease.Lease;

import jakarta.persistence.*;

import lombok.*;



import java.math.BigDecimal;

import java.time.Instant;

import java.time.LocalDate;

import java.util.UUID;



@Entity

@Table(name = "rent_payments")

@Getter @Setter

@NoArgsConstructor @AllArgsConstructor

@Builder

public class RentPayment {



    @Id

    @GeneratedValue(strategy = GenerationType.UUID)

    @Column(name = "payment_id", columnDefinition = "UUID")

    private UUID paymentId;



    @ManyToOne(optional = false, fetch = FetchType.EAGER)

    @JoinColumn(name = "lease_id", nullable = false)

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "property", "tenant", "unit"})

    private Lease lease;



    @Column(name = "month_year", nullable = false, length = 7)

    private String monthYear;



    @Column(name = "amount_expected", precision = 10, scale = 2, nullable = false)

    private BigDecimal amountExpected;



    @Column(name = "amount_paid", precision = 10, scale = 2, nullable = false)

    private BigDecimal amountPaid;



    @Column(name = "due_date", nullable = false)

    private LocalDate dueDate;



    @Column(name = "paid_date")

    private LocalDate paidDate;



    @Enumerated(EnumType.STRING)

    @Column(name = "payment_method", length = 20)

    private PaymentMethod paymentMethod;



    @Enumerated(EnumType.STRING)

    @Column(name = "payment_status", nullable = false, length = 20)

    private PaymentStatus paymentStatus;



    @Column(name = "created_at", nullable = false)

    private Instant createdAt;

    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public enum PaymentMethod {

        CASH, BANK_TRANSFER, CHECK

    }



    public enum PaymentStatus {

        PAID, PENDING, OVERDUE, PARTIAL

    }

}

