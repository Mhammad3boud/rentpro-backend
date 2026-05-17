package com.rentpro.backend.payment.stripe;

import com.rentpro.backend.payment.RentPayment;
import com.rentpro.backend.payment.profile.OwnerPaymentProfileService;
import com.rentpro.backend.security.JwtUserContext;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payments/stripe")
public class StripePaymentController {

    private final StripePaymentService stripePaymentService;
    private final OwnerPaymentProfileService ownerProfileService;
    private final StripeProperties stripeProperties;

    public StripePaymentController(StripePaymentService stripePaymentService,
                                   OwnerPaymentProfileService ownerProfileService,
                                   StripeProperties stripeProperties) {
        this.stripePaymentService = stripePaymentService;
        this.ownerProfileService = ownerProfileService;
        this.stripeProperties = stripeProperties;
    }

    /** Tenant: create a Stripe PaymentIntent and return clientSecret to the frontend. */
    @PostMapping("/intent")
    public ResponseEntity<StripeIntentResponse> createIntent(
            Authentication auth,
            @Valid @RequestBody StripeIntentRequest request) {
        UUID tenantUserId = resolveUserId(auth);
        return ResponseEntity.ok(stripePaymentService.createIntent(tenantUserId, request));
    }

    /** Tenant: confirm a succeeded PaymentIntent and record the RentPayment. */
    @PostMapping("/confirm")
    public ResponseEntity<RentPayment> confirmPayment(
            Authentication auth,
            @Valid @RequestBody StripeConfirmRequest request) {
        UUID tenantUserId = resolveUserId(auth);
        return ResponseEntity.ok(stripePaymentService.confirmPayment(tenantUserId, request));
    }

    /**
     * Owner: start Stripe Connect Express onboarding.
     * Returns the Stripe-hosted onboarding URL to redirect the owner to.
     */
    @PostMapping("/connect/onboard")
    public ResponseEntity<StripeConnectOnboardingResponse> startOnboarding(
            Authentication auth,
            @RequestBody Map<String, String> body) {
        UUID ownerUserId = resolveUserId(auth);
        String frontendBaseUrl = body.getOrDefault("frontendBaseUrl", "http://localhost:8104");
        return ResponseEntity.ok(stripePaymentService.createConnectOnboardingLink(ownerUserId, frontendBaseUrl));
    }

    /**
     * Owner: exchange the OAuth code Stripe returned after the owner authorized.
     * Frontend sends { code: "ac_xxx" } after Stripe redirects back.
     */
    @PostMapping("/connect/oauth")
    public ResponseEntity<Map<String, Boolean>> handleOAuth(
            Authentication auth,
            @RequestBody Map<String, String> body) {
        UUID ownerUserId = resolveUserId(auth);
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            throw new RuntimeException("Missing OAuth code");
        }
        boolean onboarded = stripePaymentService.exchangeOAuthCode(ownerUserId, code);
        return ResponseEntity.ok(Map.of("onboarded", onboarded));
    }

    /**
     * Stripe redirects here after the owner completes OAuth (no JWT required).
     * Exchanges the code server-side, saves the account ID, then redirects the browser
     * to the frontend settings page with a simple ?stripe_connect=success flag.
     */
    @GetMapping("/connect/callback")
    public void handleConnectCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response) throws IOException {
        String frontendBase = stripeProperties.getFrontendBaseUrl();
        try {
            UUID ownerUserId = UUID.fromString(state);
            stripePaymentService.exchangeOAuthCode(ownerUserId, code);
            response.sendRedirect(frontendBase + "/settings?stripe_connect=success");
        } catch (Exception e) {
            response.sendRedirect(frontendBase + "/settings?stripe_connect=error");
        }
    }

    /**
     * Owner: called after returning from Stripe onboarding to refresh account status.
     * Returns { onboarded: true/false }.
     */
    @PostMapping("/connect/refresh-status")
    public ResponseEntity<Map<String, Boolean>> refreshStatus(Authentication auth) {
        UUID ownerUserId = resolveUserId(auth);
        boolean onboarded = stripePaymentService.refreshConnectStatus(ownerUserId);
        return ResponseEntity.ok(Map.of("onboarded", onboarded));
    }

    private UUID resolveUserId(Authentication auth) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        return UUID.fromString(ctx.userId());
    }
}
