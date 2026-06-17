package com.hyein.payment.adapter.pg.inicis;

import com.hyein.payment.adapter.pg.PgHttpClient;
import com.hyein.payment.adapter.pg.PgHttpResponse;
import com.hyein.payment.adapter.pg.PgSignature;
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

public final class InicisPaymentGateway implements PaymentGateway {
    private final InicisProperties properties;
    private final PgHttpClient httpClient;

    public InicisPaymentGateway(InicisProperties properties, PgHttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public PgCompany company() {
        return PgCompany.INICIS;
    }

    @Override
    public CheckoutSession createCheckoutSession(Payment payment) {
        String timestamp = String.valueOf(payment.requestedAt().toEpochMilli());
        String signature = PgSignature.sha256("oid=" + payment.orderId()
                + "&price=" + payment.amount().amount().toPlainString()
                + "&timestamp=" + timestamp
                + "&signKey=" + properties.signKey());

        return new CheckoutSession(properties.checkoutUrl(), Map.ofEntries(
                Map.entry("pg", company().name()),
                Map.entry("mid", properties.mid()),
                Map.entry("oid", payment.orderId()),
                Map.entry("price", payment.amount().amount().toPlainString()),
                Map.entry("currency", payment.amount().currency().getCurrencyCode()),
                Map.entry("gopaymethod", payment.method().name()),
                Map.entry("buyername", payment.customerName()),
                Map.entry("buyertel", payment.customerPhone()),
                Map.entry("returnUrl", payment.successRedirectUrl()),
                Map.entry("closeUrl", payment.failRedirectUrl()),
                Map.entry("webhookUrl", properties.webhookUrl()),
                Map.entry("timestamp", timestamp),
                Map.entry("signature", signature)
        ), payment.successRedirectUrl(), payment.failRedirectUrl());
    }

    @Override
    public ApprovalResult approve(Payment payment, PgApprovalPayload approvalPayload) {
        Map<String, String> form = Map.of(
                "pg", company().name(),
                "mid", properties.mid(),
                "authToken", approvalPayload.required("authToken"),
                "authUrl", approvalPayload.required("authUrl"),
                "netCancelUrl", properties.netCancelUrl()
        );
        PgHttpResponse response = httpClient.postForm(properties.approveUrl(), form);
        if (!response.success()) {
            throw new IllegalStateException("INICIS approval failed: " + response.rawBody());
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
                "mid", properties.mid(),
                "tid", payment.pgTransactionId() == null ? payment.id().toString() : payment.pgTransactionId(),
                "msg", reason.message()
        );
        PgHttpResponse response = httpClient.postForm(properties.cancelUrl(), form);
        if (!response.success()) {
            throw new IllegalStateException("INICIS cancel failed: " + response.rawBody());
        }
        return new CancelResult(response.transactionId(), response.rawBody());
    }
}
