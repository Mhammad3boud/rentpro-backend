package com.rentpro.backend.payment.gateway;

import com.rentpro.backend.lease.Lease;
import com.rentpro.backend.lease.LeaseRepository;
import com.rentpro.backend.payment.RentPayment;
import com.rentpro.backend.payment.RentPaymentService;
import com.rentpro.backend.payment.dto.CheckoutSessionResponse;
import com.rentpro.backend.payment.dto.CheckoutStatusResponse;
import com.rentpro.backend.payment.dto.CreateCheckoutSessionRequest;
import com.rentpro.backend.payment.dto.CreateOrUpdatePaymentRequest;
import com.rentpro.backend.payment.profile.OwnerPaymentProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PaymentCheckoutService {
    private final PaymentCheckoutSessionRepository sessionRepository;
    private final LeaseRepository leaseRepository;
    private final PaymentGatewayService gatewayService;
    private final RentPaymentService rentPaymentService;
    private final OwnerPaymentProfileService ownerPaymentProfileService;

    public PaymentCheckoutService(PaymentCheckoutSessionRepository sessionRepository,
                                  LeaseRepository leaseRepository,
                                  PaymentGatewayService gatewayService,
                                  RentPaymentService rentPaymentService,
                                  OwnerPaymentProfileService ownerPaymentProfileService) {
        this.sessionRepository = sessionRepository;
        this.leaseRepository = leaseRepository;
        this.gatewayService = gatewayService;
        this.rentPaymentService = rentPaymentService;
        this.ownerPaymentProfileService = ownerPaymentProfileService;
    }

    public CheckoutSessionResponse createSession(UUID tenantUserId, CreateCheckoutSessionRequest request) {
        Lease lease = leaseRepository.findById(request.leaseId())
                .orElseThrow(() -> new RuntimeException("Lease not found"));
        UUID leaseTenantUserId = lease.getTenant() != null && lease.getTenant().getUser() != null
                ? lease.getTenant().getUser().getUserId()
                : null;
        if (leaseTenantUserId == null || !leaseTenantUserId.equals(tenantUserId)) {
            throw new RuntimeException("Unauthorized for lease");
        }

        PaymentCheckoutSession session = new PaymentCheckoutSession();
        session.setLease(lease);
        session.setProvider(request.provider());
        session.setMonthYear(request.monthYear());
        session.setAmountExpected(request.amountExpected());
        session.setAmountPaid(request.amountPaid());
        session.setPaidDate(request.paidDate());
        session.setReturnUrl(request.returnUrl());
        session.setCancelUrl(request.cancelUrl());
        session.setStatus(CheckoutStatus.PENDING);
        session = sessionRepository.save(session);

        UUID ownerUserId = lease.getProperty().getOwner().getUserId();
        String countryCode = ownerPaymentProfileService.getCountry(ownerUserId);

        PaymentGatewayService.GatewayCheckoutResult gatewayResult =
                gatewayService.createCheckoutSession(request, session.getCheckoutSessionId().toString(), countryCode);

        session.setProviderSessionId(gatewayResult.providerSessionId());
        session.setProviderReference(
                (gatewayResult.providerReference() == null || gatewayResult.providerReference().isBlank())
                        ? session.getCheckoutSessionId().toString()
                        : gatewayResult.providerReference()
        );
        session.setCheckoutUrl(gatewayResult.checkoutUrl());
        session.setLastProviderPayload(gatewayResult.rawPayload());
        sessionRepository.save(session);

        return new CheckoutSessionResponse(
                session.getCheckoutUrl(),
                session.getProvider(),
                session.getProviderSessionId(),
                null,
                gatewayResult.clientKey()
        );
    }

    public CheckoutStatusResponse verifyAndGetStatus(UUID tenantUserId, PaymentProvider provider, String sessionId, UUID paymentId) {
        PaymentCheckoutSession session = resolveSession(tenantUserId, provider, sessionId, paymentId);

        PaymentGatewayService.GatewayStatusResult result =
                gatewayService.fetchStatus(provider, session.getProviderSessionId());
        session.setStatus(result.status());
        session.setMessage(result.message());
        session.setLastProviderPayload(result.rawPayload());

        UUID resolvedPaymentId = paymentId;
        if (result.status() == CheckoutStatus.SUCCEEDED && paymentId == null) {
            RentPayment saved = rentPaymentService.upsertPaymentForTenant(
                    tenantUserId,
                    new CreateOrUpdatePaymentRequest(
                            session.getLease().getLeaseId(),
                            session.getMonthYear(),
                            session.getAmountExpected(),
                            session.getAmountPaid(),
                            null,
                            session.getPaidDate(),
                            mapProviderMethod(provider)
                    )
            );
            resolvedPaymentId = saved.getPaymentId();
        }
        sessionRepository.save(session);

        String currency = session.getLease() != null && session.getLease().getProperty() != null
                ? session.getLease().getProperty().getCurrency()
                : "MYR";

        return new CheckoutStatusResponse(
                session.getProvider(),
                session.getProviderSessionId(),
                resolvedPaymentId,
                session.getStatus(),
                session.getMessage(),
                session.getAmountPaid(),
                currency,
                session.getUpdatedAt()
        );
    }

    public void markByProviderReference(PaymentProvider provider, String providerReference, CheckoutStatus status, String message, String payload) {
        if (providerReference == null || providerReference.isBlank()) {
            return;
        }
        Optional<PaymentCheckoutSession> optional = sessionRepository.findByProviderAndProviderReference(provider, providerReference);
        if (optional.isEmpty()) {
            optional = sessionRepository.findByProviderAndProviderSessionId(provider, providerReference);
        }
        if (optional.isEmpty()) return;
        PaymentCheckoutSession session = optional.get();
        session.setStatus(status);
        session.setMessage(message);
        session.setLastProviderPayload(payload);
        sessionRepository.save(session);
    }

    private PaymentCheckoutSession resolveSession(UUID tenantUserId, PaymentProvider provider, String sessionId, UUID paymentId) {
        PaymentCheckoutSession session;
        if (sessionId != null && !sessionId.isBlank()) {
            session = sessionRepository.findByProviderAndProviderSessionId(provider, sessionId)
                    .orElseThrow(() -> new RuntimeException("Checkout session not found"));
        } else if (paymentId != null) {
            throw new RuntimeException("Payment lookup by paymentId is not supported without sessionId");
        } else {
            throw new RuntimeException("sessionId is required");
        }

        UUID leaseTenantUserId = session.getLease() != null
                && session.getLease().getTenant() != null
                && session.getLease().getTenant().getUser() != null
                ? session.getLease().getTenant().getUser().getUserId()
                : null;
        if (leaseTenantUserId == null || !leaseTenantUserId.equals(tenantUserId)) {
            throw new RuntimeException("Unauthorized for checkout session");
        }
        return session;
    }

    private RentPayment.PaymentMethod mapProviderMethod(PaymentProvider provider) {
        return switch (provider) {
            case PAYPAL -> RentPayment.PaymentMethod.PAYPAL;
            case ADYEN -> RentPayment.PaymentMethod.ADYEN;
            case STRIPE -> RentPayment.PaymentMethod.STRIPE;
        };
    }
}
