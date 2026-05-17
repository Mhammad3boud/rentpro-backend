package com.rentpro.backend.payment.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayProperties {

    @Value("${payment.currency:TZS}")
    private String currency;

    @Value("${payment.paypal.enabled:false}")
    private boolean paypalEnabled;

    @Value("${payment.paypal.base-url:https://api-m.sandbox.paypal.com}")
    private String paypalBaseUrl;

    @Value("${payment.paypal.client-id:}")
    private String paypalClientId;

    @Value("${payment.paypal.client-secret:}")
    private String paypalClientSecret;

    @Value("${payment.paypal.webhook-id:}")
    private String paypalWebhookId;

    @Value("${payment.adyen.enabled:false}")
    private boolean adyenEnabled;

    @Value("${payment.adyen.base-url:https://checkout-test.adyen.com}")
    private String adyenBaseUrl;

    @Value("${payment.adyen.api-key:}")
    private String adyenApiKey;

    @Value("${payment.adyen.merchant-account:}")
    private String adyenMerchantAccount;

    @Value("${payment.adyen.hmac-key:}")
    private String adyenHmacKey;

    @Value("${payment.adyen.client-key:}")
    private String adyenClientKey;

    public String currency() { return currency; }
    public boolean paypalEnabled() { return paypalEnabled; }
    public String paypalBaseUrl() { return paypalBaseUrl; }
    public String paypalClientId() { return paypalClientId; }
    public String paypalClientSecret() { return paypalClientSecret; }
    public String paypalWebhookId() { return paypalWebhookId; }
    public boolean adyenEnabled() { return adyenEnabled; }
    public String adyenBaseUrl() { return adyenBaseUrl; }
    public String adyenApiKey() { return adyenApiKey; }
    public String adyenMerchantAccount() { return adyenMerchantAccount; }
    public String adyenHmacKey() { return adyenHmacKey; }
    public String adyenClientKey() { return adyenClientKey; }
}
