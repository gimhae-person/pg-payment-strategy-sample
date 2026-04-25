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
import com.hyein.payment.port.out.PgAuthResult;

import java.util.List;
import java.util.Map;

public final class PaymentStrategyDemo {
    public static void main(String[] args) {
        PaymentService paymentService = paymentService();

        CreatePaymentResult created = paymentService.createPayment(new CreatePaymentCommand(
                "verification-20260425-0001",
                PgCompany.KCP,
                PaymentMethod.CARD,
                Money.krw(55000)
        ));

        Payment approved = paymentService.approvePayment(new ApprovePaymentCommand(
                created.paymentId(),
                new PgAuthResult(Map.of("approval_key", "kcp-auth-key", "trace_no", "trace-001"))
        ));

        System.out.println("checkoutUrl=" + created.checkoutSession().actionUrl());
        System.out.println("paymentStatus=" + approved.status());
        System.out.println("pgTransactionId=" + approved.pgTransactionId());
    }

    public static PaymentService paymentService() {
        PgHttpClient httpClient = new FakePgHttpClient();
        PaymentGatewayRegistry registry = new PaymentGatewayRegistry(List.of(
                new KcpPaymentGateway(new KcpProperties(
                        "T0000",
                        "https://testpay.kcp.co.kr/checkout",
                        "https://testpay.kcp.co.kr/approve",
                        "https://testpay.kcp.co.kr/cancel"
                ), httpClient),
                new InicisPaymentGateway(new InicisProperties(
                        "INIpayTest",
                        "test-sign-key",
                        "https://stgstdpay.inicis.com/checkout",
                        "https://stgstdpay.inicis.com/api/payAuth",
                        "https://stgstdpay.inicis.com/api/cancel",
                        "https://stgstdpay.inicis.com/api/netCancel"
                ), httpClient),
                new TossPaymentsGateway(new TossProperties(
                        "test_ck_client_key",
                        "https://pay.toss.im/checkout",
                        "https://api.tosspayments.com/v1/payments/confirm",
                        "https://api.tosspayments.com/v1/payments/cancel"
                ), httpClient)
        ));

        return new PaymentService(registry, new InMemoryPaymentRepository(), new InMemoryPaymentEventPublisher());
    }
}
