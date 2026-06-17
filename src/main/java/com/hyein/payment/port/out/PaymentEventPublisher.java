package com.hyein.payment.port.out;

import com.hyein.payment.domain.PaymentEvent;

public interface PaymentEventPublisher {
    void publish(PaymentEvent event);
}
