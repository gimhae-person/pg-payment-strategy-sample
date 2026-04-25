package com.hyein.payment.adapter.pg;

import java.util.Map;

public record PgHttpResponse(boolean success, String transactionId, String rawBody, Map<String, String> attributes) {
    public PgHttpResponse {
        attributes = Map.copyOf(attributes);
    }
}
