package com.hyein.payment.port.out;

import com.hyein.payment.domain.PaymentStatus;

public record WebhookResult(
        String orderId,
        String pgTransactionId,
        PaymentStatus targetStatus,
        String rawBody
) {
}
