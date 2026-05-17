package com.rentpro.backend.payment.profile;

import java.util.List;

/**
 * Full profile returned to the owner and used by tenants when initiating payment.
 * Sensitive fields (account numbers, wallet IDs) are visible so the tenant
 * knows where to send money for manual methods.
 */
public record OwnerPaymentProfileResponse(
        String country,
        List<String> acceptedMethods,
        String bankName,
        String bankAccountNumber,
        String bankAccountName,
        String bankSwiftCode,
        String duitnowId,
        String touchngoPhone,
        String grabpayPhone,
        String mpesaPhone,
        String airtelMoneyPhone,
        String tigoPesaPhone,
        String stripeAccountId,
        boolean stripeOnboarded
) {}
