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
import com.hyein.payment.port.out.PgAuthResult;

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
        return new CheckoutSession(properties.checkoutUrl(), Map.of(
                "pg", company().name(),
                "site_cd", properties.siteCode(),
                "ordr_idxx", payment.orderId(),
                "good_mny", payment.amount().amount().toPlainString(),
                "currency", payment.amount().currency().getCurrencyCode(),
                "pay_method", payment.method().name(),
                "signature", signature
        ));
    }

    @Override
    public ApprovalResult approve(Payment payment, PgAuthResult authResult) {
        Map<String, String> form = Map.of(
                "pg", company().name(),
                "site_cd", properties.siteCode(),
                "ordr_idxx", payment.orderId(),
                "approval_key", authResult.required("approval_key"),
                "trace_no", authResult.required("trace_no")
        );
        PgHttpResponse response = httpClient.postForm(properties.approveUrl(), form);
        if (!response.success()) {
            throw new IllegalStateException("KCP approval failed: " + response.rawBody());
        }
        return new ApprovalResult(response.transactionId(), response.rawBody());
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
