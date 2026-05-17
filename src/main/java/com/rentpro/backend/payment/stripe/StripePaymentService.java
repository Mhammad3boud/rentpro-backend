package com.rentpro.backend.payment.stripe;

import com.rentpro.backend.lease.Lease;
import com.rentpro.backend.lease.LeaseRepository;
import com.rentpro.backend.payment.RentPayment;
import com.rentpro.backend.payment.RentPaymentService;
import com.rentpro.backend.payment.dto.CreateOrUpdatePaymentRequest;
import com.rentpro.backend.payment.profile.OwnerPaymentProfileService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.PaymentIntent;
import com.stripe.net.OAuth;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentRetrieveParams;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class StripePaymentService {

    private static final String CURRENCY_MY = "myr";
    private static final String CURRENCY_TZ = "tzs";

    private final StripeProperties properties;
    private final LeaseRepository leaseRepository;
    private final OwnerPaymentProfileService ownerProfileService;
    private final RentPaymentService rentPaymentService;

    public StripePaymentService(StripeProperties properties,
                                LeaseRepository leaseRepository,
                                OwnerPaymentProfileService ownerProfileService,
                                RentPaymentService rentPaymentService) {
        this.properties = properties;
        this.leaseRepository = leaseRepository;
        this.ownerProfileService = ownerProfileService;
        this.rentPaymentService = rentPaymentService;
    }

    public StripeIntentResponse createIntent(UUID tenantUserId, StripeIntentRequest request) {
        if (!properties.isEnabled()) {
            throw new RuntimeException("Stripe payments are not enabled");
        }
        if (isBlank(properties.getSecretKey())) {
            throw new RuntimeException("Stripe is not configured");
        }

        Lease lease = leaseRepository.findById(request.leaseId())
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        UUID leaseTenantUserId = lease.getTenant() != null && lease.getTenant().getUser() != null
                ? lease.getTenant().getUser().getUserId() : null;
        if (leaseTenantUserId == null || !leaseTenantUserId.equals(tenantUserId)) {
            throw new RuntimeException("Unauthorized for lease");
        }

        UUID ownerUserId = lease.getProperty().getOwner().getUserId();
        String country = ownerProfileService.getCountry(ownerUserId);
        String currency = "TZ".equalsIgnoreCase(country) ? CURRENCY_TZ : CURRENCY_MY;

        Stripe.apiKey = properties.getSecretKey();
        try {
            long amountMinor = toMinorUnits(request.amountPaid(), currency);
            // Stripe MYR limit is 30,000 MYR (3,000,000 sen); TZS is not supported by Stripe
            if (amountMinor > 2_999_999) {
                throw new RuntimeException(
                    "Payment amount too large for online processing (" + currency.toUpperCase() + " " +
                    request.amountPaid().toPlainString() + "). Please use bank transfer or cash for large amounts.");
            }
            String ownerStripeAccountId = ownerProfileService.getStripeAccountId(ownerUserId);

            List<String> stripeMethodTypes = resolvePaymentMethodTypes(request.paymentMethod(), currency);

            PaymentIntentCreateParams.Builder intentBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountMinor)
                    .setCurrency(currency)
                    .addAllPaymentMethodType(stripeMethodTypes)
                    .putMetadata("leaseId", request.leaseId().toString())
                    .putMetadata("monthYear", request.monthYear())
                    .putMetadata("tenantUserId", tenantUserId.toString())
                    .putMetadata("amountExpected", request.amountExpected().toPlainString())
                    .putMetadata("amountPaid", request.amountPaid().toPlainString())
                    .putMetadata("currency", currency);

            // Route funds directly to owner's connected Stripe account if they have one
            if (ownerStripeAccountId != null && !ownerStripeAccountId.isBlank()
                    && ownerProfileService.isStripeOnboarded(ownerUserId)) {
                intentBuilder.setTransferData(
                        PaymentIntentCreateParams.TransferData.builder()
                                .setDestination(ownerStripeAccountId)
                                .build()
                );
            }

            PaymentIntentCreateParams params = intentBuilder.build();

            PaymentIntent intent = PaymentIntent.create(params);
            return new StripeIntentResponse(
                    intent.getClientSecret(),
                    properties.getPublishableKey(),
                    intent.getId(),
                    currency
            );
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create Stripe payment intent: " + e.getMessage(), e);
        }
    }

    public RentPayment confirmPayment(UUID tenantUserId, StripeConfirmRequest request) {
        if (!properties.isEnabled() || isBlank(properties.getSecretKey())) {
            throw new RuntimeException("Stripe is not configured");
        }

        Stripe.apiKey = properties.getSecretKey();
        try {
            PaymentIntent intent = PaymentIntent.retrieve(
                    request.paymentIntentId(),
                    PaymentIntentRetrieveParams.builder().build(),
                    null
            );

            if (!"succeeded".equals(intent.getStatus())) {
                throw new RuntimeException("Payment not completed. Status: " + intent.getStatus());
            }

            var meta = intent.getMetadata();
            String metaTenantId = meta.get("tenantUserId");
            if (metaTenantId == null || !metaTenantId.equals(tenantUserId.toString())) {
                throw new RuntimeException("Unauthorized payment confirmation");
            }

            UUID leaseId = UUID.fromString(meta.get("leaseId"));
            String monthYear = meta.get("monthYear");
            BigDecimal amountExpected = new BigDecimal(meta.get("amountExpected"));
            BigDecimal amountPaid = new BigDecimal(meta.get("amountPaid"));

            return rentPaymentService.upsertPaymentForTenant(
                    tenantUserId,
                    new CreateOrUpdatePaymentRequest(
                            leaseId,
                            monthYear,
                            amountExpected,
                            amountPaid,
                            null,
                            LocalDate.now(),
                            RentPayment.PaymentMethod.STRIPE
                    )
            );
        } catch (StripeException e) {
            throw new RuntimeException("Failed to verify Stripe payment: " + e.getMessage(), e);
        }
    }

    /**
     * Standard Connect OAuth flow (the only type available in Malaysia).
     * Returns the Stripe-hosted OAuth URL the owner should open in their browser.
     */
    public StripeConnectOnboardingResponse createConnectOnboardingLink(UUID ownerUserId, String frontendBaseUrlIgnored) {
        if (!properties.isEnabled() || isBlank(properties.getSecretKey())) {
            throw new RuntimeException("Stripe is not configured");
        }
        if (isBlank(properties.getConnectClientId())) {
            throw new RuntimeException("STRIPE_CONNECT_CLIENT_ID is not set. Get it from Stripe Dashboard → Connect → Settings.");
        }

        // Backend handles the OAuth callback so the code never touches the SPA
        String callbackUrl = properties.getBackendBaseUrl() + "/payments/stripe/connect/callback";
        String oauthUrl = "https://connect.stripe.com/oauth/authorize"
                + "?response_type=code"
                + "&client_id=" + properties.getConnectClientId()
                + "&scope=read_write"
                + "&state=" + ownerUserId
                + "&redirect_uri=" + URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8);

        return new StripeConnectOnboardingResponse(oauthUrl, null);
    }

    /**
     * Exchange OAuth code for the owner's connected Stripe account ID.
     * Called after the owner is redirected back from Stripe OAuth.
     */
    public boolean exchangeOAuthCode(UUID ownerUserId, String code) {
        if (!properties.isEnabled() || isBlank(properties.getSecretKey())) {
            throw new RuntimeException("Stripe is not configured");
        }
        Stripe.apiKey = properties.getSecretKey();
        try {
            var token = OAuth.token(Map.of("grant_type", "authorization_code", "code", code), null);
            String stripeAccountId = token.getStripeUserId();
            ownerProfileService.saveStripeConnect(ownerUserId, stripeAccountId, true);
            return true;
        } catch (StripeException e) {
            throw new RuntimeException("Failed to exchange Stripe OAuth code: " + e.getMessage(), e);
        }
    }

    public boolean refreshConnectStatus(UUID ownerUserId) {
        if (!properties.isEnabled() || isBlank(properties.getSecretKey())) return false;
        Stripe.apiKey = properties.getSecretKey();
        try {
            String accountId = ownerProfileService.getStripeAccountId(ownerUserId);
            if (isBlank(accountId)) return false;
            Account account = Account.retrieve(accountId);
            boolean onboarded = Boolean.TRUE.equals(account.getDetailsSubmitted())
                    && Boolean.TRUE.equals(account.getChargesEnabled());
            ownerProfileService.saveStripeConnect(ownerUserId, accountId, onboarded);
            return onboarded;
        } catch (StripeException e) {
            throw new RuntimeException("Failed to check Stripe Connect status: " + e.getMessage(), e);
        }
    }

    private List<String> resolvePaymentMethodTypes(String method, String currency) {
        if (method == null) return List.of("card");
        return switch (method.toUpperCase()) {
            case "FPX"     -> List.of("fpx");
            case "GRABPAY" -> List.of("grabpay");
            default        -> List.of("card");
        };
    }

    private long toMinorUnits(BigDecimal amount, String currency) {
        // TZS is a zero-decimal currency in Stripe
        if (CURRENCY_TZ.equals(currency)) {
            return amount.longValue();
        }
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }

    private boolean isBlank(String v) {
        return v == null || v.isBlank();
    }
}
