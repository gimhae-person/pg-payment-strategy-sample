package com.hyein.payment.port.out;

import java.util.Map;

public record PgApprovalPayload(Map<String, String> values) {
    public PgApprovalPayload {
        values = Map.copyOf(values);
    }

    public String required(String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing PG approval field: " + key);
        }
        return value;
    }
}
