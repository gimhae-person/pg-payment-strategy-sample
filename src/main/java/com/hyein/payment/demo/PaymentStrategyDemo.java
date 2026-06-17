package com.hyein.payment.demo;

import com.hyein.payment.adapter.event.InMemoryPaymentEventPublisher;
import com.hyein.payment.adapter.persistence.InMemoryPaymentRepository;
import com.hyein.payment.adapter.pg.FakePgHttpClient;
import com.hyein.payment.adapter.pg.PgHttpClient;
import com.hyein.payment.adapter.pg.inicis.InicisPaymentGateway;
import com.hyein.payment.adapter.pg.inicis.InicisProperties;
import com.hyein.payment.adapter.pg.kcp.KcpPaymentGateway;
import com.hyein.payment.adapter.pg.kcp.KcpProperties;
import com.hyein.payment.adapter.pg.toss.TossPaymentsGateway;
import com.hyein.payment.adapter.pg.toss.TossProperties;
import com.hyein.payment.application.PaymentGatewayRegistry;
import com.hyein.payment.application.PaymentService;
import com.hyein.payment.domain.Money;
import com.hyein.payment.domain.Payment;
import com.hyein.payment.domain.PaymentMethod;
import com.hyein.payment.domain.PgCompany;
import com.hyein.payment.port.in.ApprovePaymentCommand;
import com.hyein.payment.port.in.CreatePaymentCommand;
import com.hyein.payment.port.in.CreatePaymentResult;
import com.hyein.payment.port.in.HandleWebhookCommand;
import com.hyein.payment.port.out.PgApprovalPayload;

import java.util.List;
import java.util.Map;

public final class PaymentStrategyDemo {
    public static void main(String[] args) {
        PaymentService paymentService = paymentService();

        CreatePaymentResult created = paymentService.createPayment(new CreatePaymentCommand(
                "verification-20260425-0001",
                PgCompany.KCP,
                PaymentMethod.CARD,
                Money.krw(55000),
                "홍길동",
                "01012341234",
                "https://frontend.example/payments/success",
                "https://frontend.example/payments/fail"
        ));

        Payment approved = paymentService.approvePayment(new ApprovePaymentCommand(
                created.paymentId(),
                new PgApprovalPayload(Map.of("approval_key", "kcp-auth-key", "trace_no", "trace-001"))
        ));

        Payment reconciled = paymentService.handleWebhook(new HandleWebhookCommand(
                PgCompany.KCP,
                Map.of("X-KCP-SIGNATURE", "sample"),
                Map.of(
                        "orderId", "verification-20260425-0001",
                        "transactionId", approved.pgTransactionId(),
                        "status", "APPROVED"
                )
        ));

        System.out.println("checkoutUrl=" + created.checkoutSession().actionUrl());
        System.out.println("frontendSuccessUrl=" + created.checkoutSession().successRedirectUrl());
        System.out.println("paymentStatus=" + reconciled.status());
        System.out.println("pgTransactionId=" + reconciled.pgTransactionId());
    }

    public static PaymentService paymentService() {
        PgHttpClient httpClient = new FakePgHttpClient();
        PaymentGatewayRegistry registry = new PaymentGatewayRegistry(List.of(
                new KcpPaymentGateway(new KcpProperties(
                        "T0000",
                        "https://testpay.kcp.co.kr/checkout",
                        "https://testpay.kcp.co.kr/approve",
                        "https://testpay.kcp.co.kr/cancel",
                        "https://api.example.com/payments/webhooks/kcp"
                ), httpClient),
                new InicisPaymentGateway(new InicisProperties(
                        "INIpayTest",
                        "test-sign-key",
                        "https://stgstdpay.inicis.com/checkout",
                        "https://stgstdpay.inicis.com/api/payAuth",
                        "https://stgstdpay.inicis.com/api/cancel",
                        "https://stgstdpay.inicis.com/api/netCancel",
                        "https://api.example.com/payments/webhooks/inicis"
                ), httpClient),
                new TossPaymentsGateway(new TossProperties(
                        "test_ck_client_key",
                        "https://pay.toss.im/checkout",
                        "https://api.tosspayments.com/v1/payments/confirm",
                        "https://api.tosspayments.com/v1/payments/cancel",
                        "https://api.example.com/payments/webhooks/toss"
                ), httpClient)
        ));

        return new PaymentService(registry, new InMemoryPaymentRepository(), new InMemoryPaymentEventPublisher());
    }
}
