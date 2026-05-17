package com.rentpro.backend.payment.profile;

import java.util.List;

public record OwnerPaymentProfileRequest(
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
        String tigoPesaPhone
) {}
