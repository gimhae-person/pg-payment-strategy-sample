package com.hyein.payment.adapter.pg.toss;

import com.hyein.payment.adapter.pg.PgHttpClient;
import com.hyein.payment.adapter.pg.PgHttpResponse;
import com.hyein.payment.domain.Payment;
import com.hyein.payment.domain.PgCompany;
import com.hyein.payment.port.out.ApprovalResult;
import com.hyein.payment.port.out.CancelReason;
import com.hyein.payment.port.out.CancelResult;
import com.hyein.payment.port.out.CheckoutSession;
import com.hyein.payment.port.out.PaymentGateway;
import com.hyein.payment.port.out.PgApprovalPayload;
import com.hyein.payment.port.out.WebhookPayload;
import com.hyein.payment.port.out.WebhookResult;

import java.util.Map;

public final class TossPaymentsGateway implements PaymentGateway {
    private final TossProperties properties;
    private final PgHttpClient httpClient;

    public TossPaymentsGateway(TossProperties properties, PgHttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public PgCompany company() {
        return PgCompany.TOSS_PAYMENTS;
    }

    @Override
    public CheckoutSession createCheckoutSession(Payment payment) {
        return new CheckoutSession(properties.checkoutUrl(), Map.of(
                "pg", company().name(),
                "clientKey", properties.clientKey(),
                "orderId", payment.orderId(),
                "amount", payment.amount().amount().toPlainString(),
                "currency", payment.amount().currency().getCurrencyCode(),
                "customerName", payment.customerName(),
                "customerMobilePhone", payment.customerPhone(),
                "successUrl", payment.successRedirectUrl(),
                "failUrl", payment.failRedirectUrl(),
                "webhookUrl", properties.webhookUrl()
        ), payment.successRedirectUrl(), payment.failRedirectUrl());
    }

    @Override
    public ApprovalResult approve(Payment payment, PgApprovalPayload approvalPayload) {
        Map<String, String> form = Map.of(
                "pg", company().name(),
                "paymentKey", approvalPayload.required("paymentKey"),
                "orderId", payment.orderId(),
                "amount", payment.amount().amount().toPlainString()
        );
        PgHttpResponse response = httpClient.postForm(properties.approveUrl(), form);
        if (!response.success()) {
            throw new IllegalStateException("TossPayments approval failed: " + response.rawBody());
        }
        return new ApprovalResult(response.transactionId(), response.rawBody(), true);
    }

    @Override
    public WebhookResult parseWebhook(WebhookPayload webhookPayload) {
        String orderId = webhookPayload.requiredBody("orderId");
        String transactionId = webhookPayload.requiredBody("transactionId");
        String status = webhookPayload.requiredBody("status");
        return new WebhookResult(orderId, transactionId, switch (status) {
            case "APPROVED" -> com.hyein.payment.domain.PaymentStatus.APPROVED;
            case "CANCELLED" -> com.hyein.payment.domain.PaymentStatus.CANCELLED;
            default -> com.hyein.payment.domain.PaymentStatus.FAILED;
        }, webhookPayload.body().toString());
    }

    @Override
    public CancelResult cancel(Payment payment, CancelReason reason) {
        Map<String, String> form = Map.of(
                "pg", company().name(),
                "paymentKey", payment.pgTransactionId() == null ? payment.id().toString() : payment.pgTransactionId(),
                "cancelReason", reason.message()
        );
        PgHttpResponse response = httpClient.postForm(properties.cancelUrl(), form);
        if (!response.success()) {
            throw new IllegalStateException("TossPayments cancel failed: " + response.rawBody());
        }
        return new CancelResult(response.transactionId(), response.rawBody());
    }
}
