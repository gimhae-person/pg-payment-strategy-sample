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
import com.hyein.payment.port.out.PgAuthResult;

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

        return new CheckoutSession(properties.checkoutUrl(), Map.of(
                "pg", company().name(),
                "mid", properties.mid(),
                "oid", payment.orderId(),
                "price", payment.amount().amount().toPlainString(),
                "currency", payment.amount().currency().getCurrencyCode(),
                "gopaymethod", payment.method().name(),
                "timestamp", timestamp,
                "signature", signature
        ));
    }

    @Override
    public ApprovalResult approve(Payment payment, PgAuthResult authResult) {
        Map<String, String> form = Map.of(
                "pg", company().name(),
                "mid", properties.mid(),
                "authToken", authResult.required("authToken"),
                "authUrl", authResult.required("authUrl"),
                "netCancelUrl", properties.netCancelUrl()
        );
        PgHttpResponse response = httpClient.postForm(properties.approveUrl(), form);
        if (!response.success()) {
            throw new IllegalStateException("INICIS approval failed: " + response.rawBody());
        }
        return new ApprovalResult(response.transactionId(), response.rawBody());
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
