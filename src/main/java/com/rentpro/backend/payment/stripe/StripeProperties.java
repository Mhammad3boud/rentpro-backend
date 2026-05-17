package com.rentpro.backend.payment.stripe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripeProperties {

    @Value("${payment.stripe.enabled:false}")
    private boolean enabled;

    @Value("${payment.stripe.secret-key:}")
    private String secretKey;

    @Value("${payment.stripe.publishable-key:}")
    private String publishableKey;

    @Value("${payment.stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${payment.stripe.connect-client-id:}")
    private String connectClientId;

    @Value("${payment.stripe.backend-base-url:http://localhost:8080}")
    private String backendBaseUrl;

    @Value("${payment.stripe.frontend-base-url:http://localhost:8104}")
    private String frontendBaseUrl;

    public boolean isEnabled() { return enabled; }
    public String getSecretKey() { return secretKey; }
    public String getPublishableKey() { return publishableKey; }
    public String getWebhookSecret() { return webhookSecret; }
    public String getConnectClientId() { return connectClientId; }
    public String getBackendBaseUrl() { return backendBaseUrl; }
    public String getFrontendBaseUrl() { return frontendBaseUrl; }
}
