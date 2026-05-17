package com.rentpro.backend.payment.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentpro.backend.payment.dto.CreateCheckoutSessionRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class PaymentGatewayService {

    private final PaymentGatewayProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public PaymentGatewayService(PaymentGatewayProperties properties) {
        this.properties = properties;
    }

    public GatewayCheckoutResult createCheckoutSession(CreateCheckoutSessionRequest request, String internalSessionId, String countryCode) {
        return switch (request.provider()) {
            case PAYPAL -> createPaypalCheckout(request, internalSessionId);
            case ADYEN -> createAdyenCheckout(request, internalSessionId, countryCode);
            case STRIPE -> throw new IllegalArgumentException("Stripe uses its own payment flow, not the hosted checkout gateway");
        };
    }

    public GatewayStatusResult fetchStatus(PaymentProvider provider, String providerSessionId) {
        return switch (provider) {
            case PAYPAL -> fetchPaypalStatus(providerSessionId);
            case ADYEN -> fetchAdyenStatus(providerSessionId);
            case STRIPE -> throw new IllegalArgumentException("Stripe status is not fetched via the gateway service");
        };
    }

    public boolean verifyPaypalWebhook(JsonNode payload, String transmissionId, String transmissionTime, String certUrl, String authAlgo, String transmissionSig) {
        if (isBlank(properties.paypalWebhookId()) || isBlank(properties.paypalClientId()) || isBlank(properties.paypalClientSecret())) {
            return false;
        }
        try {
            final String token = getPaypalAccessToken();
            final String body = objectMapper.writeValueAsString(Map.of(
                    "transmission_id", transmissionId,
                    "transmission_time", transmissionTime,
                    "cert_url", certUrl,
                    "auth_algo", authAlgo,
                    "transmission_sig", transmissionSig,
                    "webhook_id", properties.paypalWebhookId(),
                    "webhook_event", payload
            ));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(properties.paypalBaseUrl() + "/v1/notifications/verify-webhook-signature"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) return false;
            JsonNode node = objectMapper.readTree(response.body());
            return "SUCCESS".equalsIgnoreCase(node.path("verification_status").asText());
        } catch (Exception ex) {
            return false;
        }
    }

    private GatewayCheckoutResult createPaypalCheckout(CreateCheckoutSessionRequest request, String internalSessionId) {
        if (!properties.paypalEnabled()) {
            throw new RuntimeException("PayPal gateway is disabled");
        }
        try {
            String token = getPaypalAccessToken();
            String body = objectMapper.writeValueAsString(Map.of(
                    "intent", "CAPTURE",
                    "purchase_units", new Object[]{
                            Map.of(
                                    "reference_id", internalSessionId,
                                    "amount", Map.of(
                                            "currency_code", properties.currency(),
                                            "value", request.amountPaid().setScale(2).toPlainString()
                                    )
                            )
                    },
                    "application_context", Map.of(
                            "return_url", request.returnUrl() + "&sessionId=" + internalSessionId,
                            "cancel_url", request.cancelUrl() + "&sessionId=" + internalSessionId
                    )
            ));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(properties.paypalBaseUrl() + "/v2/checkout/orders"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("PayPal order creation failed: " + response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String orderId = root.path("id").asText();
            String checkoutUrl = null;
            for (JsonNode link : root.path("links")) {
                if ("approve".equalsIgnoreCase(link.path("rel").asText())) {
                    checkoutUrl = link.path("href").asText();
                    break;
                }
            }
            if (isBlank(checkoutUrl)) {
                throw new RuntimeException("PayPal did not provide approval URL");
            }
            return new GatewayCheckoutResult(orderId, null, checkoutUrl, null, response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("PayPal checkout session failed", e);
        }
    }

    private GatewayCheckoutResult createAdyenCheckout(CreateCheckoutSessionRequest request, String internalSessionId, String countryCode) {
        if (!properties.adyenEnabled()) {
            throw new RuntimeException("Adyen gateway is disabled");
        }
        if (isBlank(properties.adyenApiKey()) || isBlank(properties.adyenMerchantAccount())) {
            throw new RuntimeException("Adyen is not configured");
        }
        try {
            long amountMinor = toMinorUnits(request.amountPaid());
            String body = objectMapper.writeValueAsString(Map.of(
                    "merchantAccount", properties.adyenMerchantAccount(),
                    "reference", internalSessionId,
                    "amount", Map.of("currency", properties.currency(), "value", amountMinor),
                    "returnUrl", request.returnUrl() + "&sessionId=" + internalSessionId,
                    "countryCode", (countryCode != null && !countryCode.isBlank()) ? countryCode : "MY",
                    "shopperReference", request.leaseId().toString()
            ));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(properties.adyenBaseUrl() + "/v69/sessions"))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", properties.adyenApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("Adyen session creation failed: " + response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String sessionId = root.path("id").asText(null);
            String checkoutUrl = root.path("url").asText(null);
            if (isBlank(sessionId) || isBlank(checkoutUrl)) {
                throw new RuntimeException("Adyen did not provide session id/url");
            }
            return new GatewayCheckoutResult(sessionId, root.path("reference").asText(null), checkoutUrl, properties.adyenClientKey(), response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Adyen checkout session failed", e);
        }
    }

    private GatewayStatusResult fetchPaypalStatus(String paypalOrderId) {
        if (isBlank(paypalOrderId)) {
            throw new RuntimeException("Missing PayPal order id");
        }
        try {
            String token = getPaypalAccessToken();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(properties.paypalBaseUrl() + "/v2/checkout/orders/" + paypalOrderId))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("PayPal status fetch failed");
            }
            JsonNode root = objectMapper.readTree(response.body());
            String status = root.path("status").asText("PENDING");
            CheckoutStatus mapped = switch (status.toUpperCase()) {
                // APPROVED means payer approved the order, but capture may not be completed yet.
                case "COMPLETED" -> CheckoutStatus.SUCCEEDED;
                case "APPROVED" -> CheckoutStatus.PENDING;
                case "VOIDED" -> CheckoutStatus.CANCELLED;
                case "FAILED", "DENIED" -> CheckoutStatus.FAILED;
                default -> CheckoutStatus.PENDING;
            };
            return new GatewayStatusResult(mapped, status, response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to verify PayPal status", e);
        }
    }

    private GatewayStatusResult fetchAdyenStatus(String adyenSessionId) {
        if (isBlank(adyenSessionId)) {
            throw new RuntimeException("Missing Adyen session id");
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(properties.adyenBaseUrl() + "/v69/sessions/" + adyenSessionId))
                    .header("X-API-Key", properties.adyenApiKey())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("Adyen status fetch failed");
            }
            JsonNode root = objectMapper.readTree(response.body());
            String resultCode = root.path("resultCode").asText("PENDING");
            CheckoutStatus mapped = switch (resultCode.toUpperCase()) {
                case "AUTHORISED", "RECEIVED" -> CheckoutStatus.SUCCEEDED;
                case "CANCELLED" -> CheckoutStatus.CANCELLED;
                case "ERROR", "REFUSED" -> CheckoutStatus.FAILED;
                default -> CheckoutStatus.PENDING;
            };
            return new GatewayStatusResult(mapped, resultCode, response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to verify Adyen status", e);
        }
    }

    private String getPaypalAccessToken() throws IOException, InterruptedException {
        final String creds = Base64.getEncoder().encodeToString((properties.paypalClientId() + ":" + properties.paypalClientSecret())
                .getBytes(StandardCharsets.UTF_8));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(properties.paypalBaseUrl() + "/v1/oauth2/token"))
                .header("Authorization", "Basic " + creds)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();
        HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new RuntimeException("Failed to fetch PayPal access token. status="
                    + response.statusCode() + ", body=" + response.body());
        }
        JsonNode root = objectMapper.readTree(response.body());
        return root.path("access_token").asText();
    }

    private long toMinorUnits(BigDecimal value) {
        return value.multiply(BigDecimal.valueOf(100)).longValue();
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    public record GatewayCheckoutResult(String providerSessionId, String providerReference, String checkoutUrl, String clientKey, String rawPayload) {}
    public record GatewayStatusResult(CheckoutStatus status, String message, String rawPayload) {}
}
