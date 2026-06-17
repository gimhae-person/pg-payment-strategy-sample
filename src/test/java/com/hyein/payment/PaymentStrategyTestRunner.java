package com.hyein.payment;

import com.hyein.payment.adapter.event.InMemoryPaymentEventPublisher;
import com.hyein.payment.adapter.persistence.InMemoryPaymentRepository;
import com.hyein.payment.adapter.pg.FakePgHttpClient;
import com.hyein.payment.adapter.pg.inicis.InicisPaymentGateway;
import com.hyein.payment.adapter.pg.inicis.InicisProperties;
import com.hyein.payment.adapter.pg.kcp.KcpPaymentGateway;
import com.hyein.payment.adapter.pg.kcp.KcpProperties;
import com.hyein.payment.application.PaymentGatewayRegistry;
import com.hyein.payment.application.PaymentService;
import com.hyein.payment.domain.Money;
import com.hyein.payment.domain.Payment;
import com.hyein.payment.domain.PaymentMethod;
import com.hyein.payment.domain.PaymentStatus;
import com.hyein.payment.domain.PgCompany;
import com.hyein.payment.port.in.ApprovePaymentCommand;
import com.hyein.payment.port.in.CancelPaymentCommand;
import com.hyein.payment.port.in.CreatePaymentCommand;
import com.hyein.payment.port.in.CreatePaymentResult;
import com.hyein.payment.port.in.HandleWebhookCommand;
import com.hyein.payment.port.out.CancelReason;
import com.hyein.payment.port.out.PgApprovalPayload;

import java.util.List;
import java.util.Map;

public final class PaymentStrategyTestRunner {
    public static void main(String[] args) {
        createsCheckoutSessionWithSelectedGateway();
        approvesKcpPaymentWithoutConcreteDependencyInService();
        reconcilesPaymentWithWebhook();
        cancelsInicisPayment();
        rejectsDuplicateGatewayRegistration();
        System.out.println("All tests passed");
    }

    private static void createsCheckoutSessionWithSelectedGateway() {
        Fixture fixture = fixture();
        CreatePaymentResult result = fixture.service.createPayment(new CreatePaymentCommand(
                "order-kcp-1",
                PgCompany.KCP,
                PaymentMethod.CARD,
                Money.krw(10000),
                "김결제",
                "01011112222",
                "https://frontend.example/success",
                "https://frontend.example/fail"
        ));

        assertEquals("https://kcp.example/checkout", result.checkoutSession().actionUrl());
        assertEquals("KCP", result.checkoutSession().formFields().get("pg"));
        assertEquals("김결제", result.checkoutSession().formFields().get("buyr_name"));
        assertEquals(1, fixture.publisher.events().size());
    }

    private static void approvesKcpPaymentWithoutConcreteDependencyInService() {
        Fixture fixture = fixture();
        CreatePaymentResult created = fixture.service.createPayment(new CreatePaymentCommand(
                "order-kcp-2",
                PgCompany.KCP,
                PaymentMethod.CARD,
                Money.krw(25000),
                "김결제",
                "01011112222",
                "https://frontend.example/success",
                "https://frontend.example/fail"
        ));

        Payment approved = fixture.service.approvePayment(new ApprovePaymentCommand(
                created.paymentId(),
                new PgApprovalPayload(Map.of("approval_key", "approval", "trace_no", "trace"))
        ));

        assertEquals(PaymentStatus.APPROVED, approved.status());
        assertTrue(approved.pgTransactionId().startsWith("KCP-"), "KCP transaction id should be returned");
    }

    private static void reconcilesPaymentWithWebhook() {
        Fixture fixture = fixture();
        CreatePaymentResult created = fixture.service.createPayment(new CreatePaymentCommand(
                "order-kcp-3",
                PgCompany.KCP,
                PaymentMethod.CARD,
                Money.krw(15000),
                "김웹훅",
                "01033334444",
                "https://frontend.example/success",
                "https://frontend.example/fail"
        ));

        Payment approved = fixture.service.approvePayment(new ApprovePaymentCommand(
                created.paymentId(),
                new PgApprovalPayload(Map.of("approval_key", "approval", "trace_no", "trace"))
        ));

        Payment reconciled = fixture.service.handleWebhook(new HandleWebhookCommand(
                PgCompany.KCP,
                Map.of("X-KCP-SIGNATURE", "sample"),
                Map.of(
                        "orderId", "order-kcp-3",
                        "transactionId", approved.pgTransactionId(),
                        "status", "APPROVED"
                )
        ));

        assertEquals(PaymentStatus.APPROVED, reconciled.status());
    }

    private static void cancelsInicisPayment() {
        Fixture fixture = fixture();
        CreatePaymentResult created = fixture.service.createPayment(new CreatePaymentCommand(
                "order-inicis-1",
                PgCompany.INICIS,
                PaymentMethod.CARD,
                Money.krw(33000),
                "이니시스유저",
                "01055556666",
                "https://frontend.example/success",
                "https://frontend.example/fail"
        ));

        Payment approved = fixture.service.approvePayment(new ApprovePaymentCommand(
                created.paymentId(),
                new PgApprovalPayload(Map.of("authToken", "token", "authUrl", "https://auth.example"))
        ));
        Payment cancelled = fixture.service.cancelPayment(new CancelPaymentCommand(
                approved.id(),
                new CancelReason("user requested")
        ));

        assertEquals(PaymentStatus.CANCELLED, cancelled.status());
    }

    private static void rejectsDuplicateGatewayRegistration() {
        try {
            new PaymentGatewayRegistry(List.of(kcpGateway(), kcpGateway()));
            throw new AssertionError("Expected duplicated gateway registration to fail");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Duplicated"), "error message should mention duplicated gateway");
        }
    }

    private static Fixture fixture() {
        InMemoryPaymentRepository repository = new InMemoryPaymentRepository();
        InMemoryPaymentEventPublisher publisher = new InMemoryPaymentEventPublisher();
        PaymentGatewayRegistry registry = new PaymentGatewayRegistry(List.of(kcpGateway(), inicisGateway()));
        return new Fixture(new PaymentService(registry, repository, publisher), publisher);
    }

    private static KcpPaymentGateway kcpGateway() {
        return new KcpPaymentGateway(new KcpProperties(
                "T0000",
                "https://kcp.example/checkout",
                "https://kcp.example/approve",
                "https://kcp.example/cancel",
                "https://api.example.com/payments/webhooks/kcp"
        ), new FakePgHttpClient());
    }

    private static InicisPaymentGateway inicisGateway() {
        return new InicisPaymentGateway(new InicisProperties(
                "INIpayTest",
                "sign-key",
                "https://inicis.example/checkout",
                "https://inicis.example/approve",
                "https://inicis.example/cancel",
                "https://inicis.example/net-cancel",
                "https://api.example.com/payments/webhooks/inicis"
        ), new FakePgHttpClient());
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record Fixture(PaymentService service, InMemoryPaymentEventPublisher publisher) {
    }
}
