package com.hyein.payment.port.in;

import com.hyein.payment.port.out.CheckoutSession;

import java.util.UUID;

public record CreatePaymentResult(UUID paymentId, CheckoutSession checkoutSession) {
}
