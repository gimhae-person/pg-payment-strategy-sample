package com.hyein.payment.port.out;

public record CancelResult(
        String pgTransactionId,
        String rawBody
) {
}
