package com.hyein.payment.port.out;

import java.util.Map;

public record WebhookPayload(
        Map<String, String> headers,
        Map<String, String> body
) {
    public WebhookPayload {
        headers = Map.copyOf(headers);
        body = Map.copyOf(body);
    }

    public String requiredBody(String key) {
        String value = body.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing webhook field: " + key);
        }
        return value;
    }
}
