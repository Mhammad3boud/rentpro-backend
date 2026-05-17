package com.rentpro.backend.payment.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments/webhooks")
public class PaymentWebhookController {

    private final PaymentCheckoutService paymentCheckoutService;
    private final PaymentGatewayService paymentGatewayService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentWebhookController(PaymentCheckoutService paymentCheckoutService,
                                    PaymentGatewayService paymentGatewayService) {
        this.paymentCheckoutService = paymentCheckoutService;
        this.paymentGatewayService = paymentGatewayService;
    }

    @PostMapping("/paypal")
    public ResponseEntity<Map<String, String>> paypalWebhook(
            @RequestHeader(value = "paypal-transmission-id", required = false) String transmissionId,
            @RequestHeader(value = "paypal-transmission-time", required = false) String transmissionTime,
            @RequestHeader(value = "paypal-cert-url", required = false) String certUrl,
            @RequestHeader(value = "paypal-auth-algo", required = false) String authAlgo,
            @RequestHeader(value = "paypal-transmission-sig", required = false) String transmissionSig,
            @RequestBody JsonNode payload) {

        boolean valid = paymentGatewayService.verifyPaypalWebhook(
                payload, transmissionId, transmissionTime, certUrl, authAlgo, transmissionSig
        );
        if (!valid) {
            return ResponseEntity.status(400).body(Map.of("status", "invalid"));
        }

        final String eventType = payload.path("event_type").asText("");
        final String resourceId = payload.path("resource").path("id").asText("");
        String internalReference = payload.path("resource")
                .path("purchase_units")
                .path(0)
                .path("reference_id")
                .asText("");
        if (internalReference.isBlank()) {
            internalReference = resourceId;
        }
        if ("CHECKOUT.ORDER.APPROVED".equalsIgnoreCase(eventType) || "PAYMENT.CAPTURE.COMPLETED".equalsIgnoreCase(eventType)) {
            paymentCheckoutService.markByProviderReference(PaymentProvider.PAYPAL, internalReference, CheckoutStatus.SUCCEEDED, eventType, payload.toString());
        } else if ("CHECKOUT.ORDER.VOIDED".equalsIgnoreCase(eventType)) {
            paymentCheckoutService.markByProviderReference(PaymentProvider.PAYPAL, internalReference, CheckoutStatus.CANCELLED, eventType, payload.toString());
        }
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/adyen")
    public ResponseEntity<Map<String, String>> adyenWebhook(@RequestBody String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root.path("notificationItems");
            if (!items.isArray()) {
                return ResponseEntity.badRequest().body(Map.of("status", "invalid"));
            }
            for (JsonNode item : items) {
                JsonNode notification = item.path("NotificationRequestItem");
                String eventCode = notification.path("eventCode").asText("");
                String success = notification.path("success").asText("false");
                String merchantReference = notification.path("merchantReference").asText("");
                if (merchantReference.isBlank()) {
                    merchantReference = notification.path("pspReference").asText("");
                }
                if ("AUTHORISATION".equalsIgnoreCase(eventCode)) {
                    CheckoutStatus status = "true".equalsIgnoreCase(success) ? CheckoutStatus.SUCCEEDED : CheckoutStatus.FAILED;
                    paymentCheckoutService.markByProviderReference(PaymentProvider.ADYEN, merchantReference, status, eventCode, notification.toString());
                } else if ("CANCELLATION".equalsIgnoreCase(eventCode)) {
                    paymentCheckoutService.markByProviderReference(PaymentProvider.ADYEN, merchantReference, CheckoutStatus.CANCELLED, eventCode, notification.toString());
                }
            }
            return ResponseEntity.ok(Map.of("status", "[accepted]"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("status", "invalid"));
        }
    }
}
