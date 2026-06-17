package com.hyein.payment.port.out;

public record ApprovalResult(
        String pgTransactionId,
        String rawBody,
        boolean awaitingWebhookConfirmation
) {
}
