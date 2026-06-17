package com.hyein.payment.adapter.pg.kcp;

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

public final class KcpPaymentGateway implements PaymentGateway {
    private final KcpProperties properties;
    private final PgHttpClient httpClient;

    public KcpPaymentGateway(KcpProperties properties, PgHttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public PgCompany company() {
        return PgCompany.KCP;
    }

    @Override
    public CheckoutSession createCheckoutSession(Payment payment) {
        String signature = PgSignature.sha256(properties.siteCode() + payment.orderId() + payment.amount().amount());
        return new CheckoutSession(properties.checkoutUrl(), Map.ofEntries(
                Map.entry("pg", company().name()),
                Map.entry("site_cd", properties.siteCode()),
                Map.entry("ordr_idxx", payment.orderId()),
                Map.entry("good_mny", payment.amount().amount().toPlainString()),
                Map.entry("currency", payment.amount().currency().getCurrencyCode()),
                Map.entry("pay_method", payment.method().name()),
                Map.entry("buyr_name", payment.customerName()),
                Map.entry("buyr_tel1", payment.customerPhone()),
                Map.entry("Ret_URL", payment.successRedirectUrl()),
                Map.entry("failUrl", payment.failRedirectUrl()),
                Map.entry("webhookUrl", properties.webhookUrl()),
                Map.entry("signature", signature)
        ), payment.successRedirectUrl(), payment.failRedirectUrl());
    }

    @Override
    public ApprovalResult approve(Payment payment, PgApprovalPayload approvalPayload) {
        Map<String, String> form = Map.of(
                "pg", company().name(),
                "site_cd", properties.siteCode(),
                "ordr_idxx", payment.orderId(),
                "approval_key", approvalPayload.required("approval_key"),
                "trace_no", approvalPayload.required("trace_no")
        );
        PgHttpResponse response = httpClient.postForm(properties.approveUrl(), form);
        if (!response.success()) {
            throw new IllegalStateException("KCP approval failed: " + response.rawBody());
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
                "site_cd", properties.siteCode(),
                "tno", payment.pgTransactionId() == null ? payment.id().toString() : payment.pgTransactionId(),
                "mod_desc", reason.message()
        );
        PgHttpResponse response = httpClient.postForm(properties.cancelUrl(), form);
        if (!response.success()) {
            throw new IllegalStateException("KCP cancel failed: " + response.rawBody());
        }
        return new CancelResult(response.transactionId(), response.rawBody());
    }
}
