package com.hyein.payment.application;

import com.hyein.payment.domain.Payment;
import com.hyein.payment.domain.PaymentEvent;
import com.hyein.payment.port.in.ApprovePaymentCommand;
import com.hyein.payment.port.in.CancelPaymentCommand;
import com.hyein.payment.port.in.CreatePaymentCommand;
import com.hyein.payment.port.in.CreatePaymentResult;
import com.hyein.payment.port.in.HandleWebhookCommand;
import com.hyein.payment.port.out.ApprovalResult;
import com.hyein.payment.port.out.CancelResult;
import com.hyein.payment.port.out.CheckoutSession;
import com.hyein.payment.port.out.PaymentEventPublisher;
import com.hyein.payment.port.out.PaymentGateway;
import com.hyein.payment.port.out.PaymentRepository;
import com.hyein.payment.port.out.WebhookPayload;
import com.hyein.payment.port.out.WebhookResult;

import java.util.Map;
import java.util.UUID;

public final class PaymentService {
    private final PaymentGatewayRegistry gatewayRegistry;
    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

    public PaymentService(
            PaymentGatewayRegistry gatewayRegistry,
            PaymentRepository paymentRepository,
            PaymentEventPublisher eventPublisher
    ) {
        this.gatewayRegistry = gatewayRegistry;
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    public CreatePaymentResult createPayment(CreatePaymentCommand command) {
        Payment payment = Payment.request(
                command.orderId(),
                command.pgCompany(),
                command.method(),
                command.amount(),
                command.customerName(),
                command.customerPhone(),
                command.successRedirectUrl(),
                command.failRedirectUrl()
        );
        paymentRepository.save(payment);

        PaymentGateway gateway = gatewayRegistry.get(payment.pgCompany());
        CheckoutSession checkoutSession = gateway.createCheckoutSession(payment);

        payment.checkoutCreated();
        paymentRepository.save(payment);
        eventPublisher.publish(PaymentEvent.of(payment, "PAYMENT_CHECKOUT_CREATED", Map.of(
                "pgCompany", payment.pgCompany().name(),
                "actionUrl", checkoutSession.actionUrl(),
                "successRedirectUrl", checkoutSession.successRedirectUrl(),
                "failRedirectUrl", checkoutSession.failRedirectUrl()
        )));

        return new CreatePaymentResult(payment.id(), checkoutSession);
    }

    public Payment approvePayment(ApprovePaymentCommand command) {
        Payment payment = findPayment(command.paymentId());
        PaymentGateway gateway = gatewayRegistry.get(payment.pgCompany());

        try {
            ApprovalResult approvalResult = gateway.approve(payment, command.approvalPayload());
            payment.approve(approvalResult.pgTransactionId());
            paymentRepository.save(payment);
            eventPublisher.publish(PaymentEvent.of(payment, "PAYMENT_APPROVED", Map.of(
                "pgCompany", payment.pgCompany().name(),
                "pgTransactionId", approvalResult.pgTransactionId(),
                "confirmedBy", approvalResult.awaitingWebhookConfirmation() ? "client+webhook" : "client"
            )));
            return payment;
        } catch (RuntimeException exception) {
            payment.fail();
            paymentRepository.save(payment);
            eventPublisher.publish(PaymentEvent.of(payment, "PAYMENT_APPROVAL_FAILED", Map.of(
                    "pgCompany", payment.pgCompany().name(),
                    "reason", exception.getMessage()
            )));
            throw exception;
        }
    }

    public Payment cancelPayment(CancelPaymentCommand command) {
        Payment payment = findPayment(command.paymentId());
        PaymentGateway gateway = gatewayRegistry.get(payment.pgCompany());

        CancelResult cancelResult = gateway.cancel(payment, command.reason());
        payment.cancel();
        paymentRepository.save(payment);
        eventPublisher.publish(PaymentEvent.of(payment, "PAYMENT_CANCELLED", Map.of(
                "pgCompany", payment.pgCompany().name(),
                "pgTransactionId", cancelResult.pgTransactionId()
        )));
        return payment;
    }

    public Payment handleWebhook(HandleWebhookCommand command) {
        PaymentGateway gateway = gatewayRegistry.get(command.pgCompany());
        WebhookResult webhookResult = gateway.parseWebhook(new WebhookPayload(command.headers(), command.body()));

        Payment payment = paymentRepository.findByOrderId(webhookResult.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for orderId: " + webhookResult.orderId()));

        switch (webhookResult.targetStatus()) {
            case APPROVED -> payment.approve(webhookResult.pgTransactionId());
            case FAILED -> payment.fail();
            case CANCELLED -> payment.cancel();
            default -> throw new IllegalArgumentException("Unsupported webhook target status: " + webhookResult.targetStatus());
        }

        paymentRepository.save(payment);
        eventPublisher.publish(PaymentEvent.of(payment, "PAYMENT_WEBHOOK_RECONCILED", Map.of(
                "pgCompany", payment.pgCompany().name(),
                "pgTransactionId", webhookResult.pgTransactionId(),
                "status", webhookResult.targetStatus().name()
        )));
        return payment;
    }

    private Payment findPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    }
}
