package com.hyein.payment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Payment {
    private final UUID id;
    private final String orderId;
    private final PgCompany pgCompany;
    private final PaymentMethod method;
    private final Money amount;
    private final Instant requestedAt;
    private PaymentStatus status;
    private String pgTransactionId;

    private Payment(UUID id, String orderId, PgCompany pgCompany, PaymentMethod method, Money amount, Instant requestedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.orderId = requireText(orderId, "orderId");
        this.pgCompany = Objects.requireNonNull(pgCompany, "pgCompany");
        this.method = Objects.requireNonNull(method, "method");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
        this.status = PaymentStatus.READY;
    }

    public static Payment request(String orderId, PgCompany pgCompany, PaymentMethod method, Money amount) {
        return new Payment(UUID.randomUUID(), orderId, pgCompany, method, amount, Instant.now());
    }

    public void checkoutCreated() {
        ensureStatus(PaymentStatus.READY);
        this.status = PaymentStatus.CHECKOUT_CREATED;
    }

    public void approve(String pgTransactionId) {
        ensureStatus(PaymentStatus.CHECKOUT_CREATED);
        this.pgTransactionId = requireText(pgTransactionId, "pgTransactionId");
        this.status = PaymentStatus.APPROVED;
    }

    public void cancel() {
        if (status != PaymentStatus.APPROVED && status != PaymentStatus.CHECKOUT_CREATED) {
            throw new IllegalStateException("Only approved or checkout-created payments can be cancelled");
        }
        this.status = PaymentStatus.CANCELLED;
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
    }

    public UUID id() {
        return id;
    }

    public String orderId() {
        return orderId;
    }

    public PgCompany pgCompany() {
        return pgCompany;
    }

    public PaymentMethod method() {
        return method;
    }

    public Money amount() {
        return amount;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public PaymentStatus status() {
        return status;
    }

    public String pgTransactionId() {
        return pgTransactionId;
    }

    private void ensureStatus(PaymentStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Expected payment status " + expected + " but was " + status);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
