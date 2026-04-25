package com.hyein.payment.adapter.event;

import com.hyein.payment.domain.PaymentEvent;
import com.hyein.payment.port.out.PaymentEventPublisher;

import java.util.ArrayList;
import java.util.List;

public final class InMemoryPaymentEventPublisher implements PaymentEventPublisher {
    private final List<PaymentEvent> events = new ArrayList<>();

    @Override
    public void publish(PaymentEvent event) {
        events.add(event);
    }

    public List<PaymentEvent> events() {
        return List.copyOf(events);
    }
}
