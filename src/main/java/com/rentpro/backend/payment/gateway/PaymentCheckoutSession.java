package com.rentpro.backend.payment.gateway;

import com.rentpro.backend.lease.Lease;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payment_checkout_sessions")
@Getter
@Setter
public class PaymentCheckoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "checkout_session_id", columnDefinition = "UUID")
    private UUID checkoutSessionId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "lease_id", nullable = false)
    private Lease lease;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private PaymentProvider provider;

    @Column(name = "month_year", nullable = false, length = 7)
    private String monthYear;

    @Column(name = "amount_expected", precision = 10, scale = 2, nullable = false)
    private BigDecimal amountExpected;

    @Column(name = "amount_paid", precision = 10, scale = 2, nullable = false)
    private BigDecimal amountPaid;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "return_url", nullable = false, length = 1024)
    private String returnUrl;

    @Column(name = "cancel_url", nullable = false, length = 1024)
    private String cancelUrl;

    @Column(name = "provider_session_id", length = 255)
    private String providerSessionId;

    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    @Column(name = "checkout_url", length = 2048)
    private String checkoutUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CheckoutStatus status = CheckoutStatus.PENDING;

    @Column(name = "message", length = 1024)
    private String message;

    @Column(name = "last_provider_payload", columnDefinition = "TEXT")
    private String lastProviderPayload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void prePersist() {
        final Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = CheckoutStatus.PENDING;
        }
    }

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
