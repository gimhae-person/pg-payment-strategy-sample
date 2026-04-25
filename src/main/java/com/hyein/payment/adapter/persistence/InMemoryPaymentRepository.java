package com.hyein.payment.adapter.persistence;

import com.hyein.payment.domain.Payment;
import com.hyein.payment.port.out.PaymentRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryPaymentRepository implements PaymentRepository {
    private final Map<UUID, Payment> store = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        store.put(payment.id(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }
}
