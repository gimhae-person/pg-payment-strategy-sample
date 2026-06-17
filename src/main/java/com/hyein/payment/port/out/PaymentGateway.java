package com.hyein.payment.port.out;

import com.hyein.payment.domain.Payment;
import com.hyein.payment.domain.PgCompany;

public interface PaymentGateway {
    PgCompany company();

    CheckoutSession createCheckoutSession(Payment payment);

    ApprovalResult approve(Payment payment, PgApprovalPayload approvalPayload);

    WebhookResult parseWebhook(WebhookPayload webhookPayload);

    CancelResult cancel(Payment payment, CancelReason reason);
}
