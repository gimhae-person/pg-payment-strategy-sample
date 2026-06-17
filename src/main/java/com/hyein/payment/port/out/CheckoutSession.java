package com.hyein.payment.port.out;

import java.util.Map;

public record CheckoutSession(
        String actionUrl,
        Map<String, String> formFields,
        String successRedirectUrl,
        String failRedirectUrl
) {
    public CheckoutSession {
        formFields = Map.copyOf(formFields);
    }
}
