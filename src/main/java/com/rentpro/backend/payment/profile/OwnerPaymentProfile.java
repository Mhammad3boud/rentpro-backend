package com.rentpro.backend.payment.profile;

import com.rentpro.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "owner_payment_profiles")
@Getter
@Setter
public class OwnerPaymentProfile {

    @Id
    @UuidGenerator
    @Column(name = "profile_id", columnDefinition = "UUID")
    private UUID profileId;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** ISO 3166-1 alpha-2: MY, TZ */
    @Column(name = "country", nullable = false, length = 2)
    private String country = "MY";

    /** Comma-separated PaymentMethod names the owner accepts, e.g. "FPX,MPESA,BANK_TRANSFER" */
    @Column(name = "accepted_methods", nullable = false)
    private String acceptedMethods = "";

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_account_name", length = 100)
    private String bankAccountName;

    @Column(name = "bank_swift_code", length = 20)
    private String bankSwiftCode;

    @Column(name = "duitnow_id", length = 50)
    private String duitnowId;

    @Column(name = "touchngo_phone", length = 20)
    private String touchngoPhone;

    @Column(name = "grabpay_phone", length = 20)
    private String grabpayPhone;

    @Column(name = "mpesa_phone", length = 20)
    private String mpesaPhone;

    @Column(name = "airtel_money_phone", length = 20)
    private String airtelMoneyPhone;

    @Column(name = "tigo_pesa_phone", length = 20)
    private String tigoPesaPhone;

    /** Stripe Connect Express account ID, e.g. acct_xxx. Null until owner completes onboarding. */
    @Column(name = "stripe_account_id", length = 255)
    private String stripeAccountId;

    /** True once the owner has completed Stripe Connect onboarding. */
    @Column(name = "stripe_onboarded", nullable = false)
    private boolean stripeOnboarded = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
