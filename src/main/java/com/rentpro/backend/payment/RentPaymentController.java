package com.rentpro.backend.payment;

import com.rentpro.backend.payment.dto.CreateOrUpdatePaymentRequest;
import com.rentpro.backend.security.JwtUserContext;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class RentPaymentController {

    private final RentPaymentService rentPaymentService;

    public RentPaymentController(RentPaymentService rentPaymentService) {
        this.rentPaymentService = rentPaymentService;
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
}
