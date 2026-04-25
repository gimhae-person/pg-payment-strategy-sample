package com.hyein.payment.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PaymentEvent(
        UUID paymentId,
        String type,
        Map<String, String> payload,
        Instant occurredAt
) {
    public static PaymentEvent of(Payment payment, String type, Map<String, String> payload) {
        return new PaymentEvent(payment.id(), type, Map.copyOf(payload), Instant.now());
    }
}
