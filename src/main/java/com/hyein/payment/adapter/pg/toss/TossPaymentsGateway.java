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
import com.hyein.payment.port.out.PgAuthResult;

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
                "currency", payment.amount().currency().getCurrencyCode()
        ));
    }

    @Override
    public ApprovalResult approve(Payment payment, PgAuthResult authResult) {
        Map<String, String> form = Map.of(
                "pg", company().name(),
                "paymentKey", authResult.required("paymentKey"),
                "orderId", payment.orderId(),
                "amount", payment.amount().amount().toPlainString()
        );
        PgHttpResponse response = httpClient.postForm(properties.approveUrl(), form);
        if (!response.success()) {
            throw new IllegalStateException("TossPayments approval failed: " + response.rawBody());
        }
        return new ApprovalResult(response.transactionId(), response.rawBody());
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
