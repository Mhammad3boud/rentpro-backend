package com.rentpro.backend.payment;

import com.rentpro.backend.payment.dto.CreateOrUpdatePaymentRequest;
import com.rentpro.backend.payment.dto.CheckoutSessionResponse;
import com.rentpro.backend.payment.dto.CheckoutStatusResponse;
import com.rentpro.backend.payment.dto.CreateCheckoutSessionRequest;
import com.rentpro.backend.payment.gateway.PaymentCheckoutService;
import com.rentpro.backend.payment.gateway.PaymentProvider;
import com.rentpro.backend.security.JwtUserContext;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class RentPaymentController {

    private final RentPaymentService rentPaymentService;
    private final PaymentCheckoutService paymentCheckoutService;

    public RentPaymentController(RentPaymentService rentPaymentService,
                                 PaymentCheckoutService paymentCheckoutService) {
        this.rentPaymentService = rentPaymentService;
        this.paymentCheckoutService = paymentCheckoutService;
    }

    // Create or update payment record
    @PostMapping
    public RentPayment createOrUpdatePayment(Authentication auth,
                                             @RequestBody CreateOrUpdatePaymentRequest request) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        return rentPaymentService.upsertPayment(ownerId, request);
    }

    // Get payments for a specific lease
    @GetMapping("/leases/{leaseId}")
    public List<RentPayment> getLeasePayments(Authentication auth,
                                              @PathVariable UUID leaseId) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        return rentPaymentService.getLeasePayments(ownerId, leaseId);
    }

    // Get current user's payment history (for tenants)
    @GetMapping("/my-payments")
    public List<RentPayment> getCurrentUserPayments(Authentication auth) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID tenantUserId = UUID.fromString(ctx.userId());
        return rentPaymentService.getTenantPayments(tenantUserId);
    }

    // Get specific payment by ID
    @GetMapping("/{paymentId}")
    public RentPayment getPaymentById(@PathVariable UUID paymentId) {
        return rentPaymentService.getPaymentById(paymentId);
    }

    // Owner marks a payment as unpaid
    @PatchMapping("/{paymentId}/unpaid")
    public RentPayment markPaymentAsUnpaid(Authentication auth,
                                           @PathVariable UUID paymentId) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        return rentPaymentService.markPaymentAsUnpaid(ownerId, paymentId);
    }

    // Owner deletes a payment record
    @DeleteMapping("/{paymentId}")
    public void deletePayment(Authentication auth,
                               @PathVariable UUID paymentId) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID ownerId = UUID.fromString(ctx.userId());
        rentPaymentService.deletePayment(ownerId, paymentId);
    }

    @PostMapping("/checkout/session")
    public CheckoutSessionResponse createCheckoutSession(Authentication auth,
                                                         @Valid @RequestBody CreateCheckoutSessionRequest request) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID tenantUserId = UUID.fromString(ctx.userId());
        return paymentCheckoutService.createSession(tenantUserId, request);
    }

    @GetMapping("/checkout/status")
    public CheckoutStatusResponse getCheckoutStatus(Authentication auth,
                                                    @RequestParam PaymentProvider provider,
                                                    @RequestParam(required = false) String sessionId,
                                                    @RequestParam(required = false) UUID paymentId) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID tenantUserId = UUID.fromString(ctx.userId());
        return paymentCheckoutService.verifyAndGetStatus(tenantUserId, provider, sessionId, paymentId);
    }
}
