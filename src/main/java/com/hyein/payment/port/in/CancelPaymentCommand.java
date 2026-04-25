package com.hyein.payment.port.in;

import com.hyein.payment.port.out.CancelReason;

import java.util.UUID;

public record CancelPaymentCommand(UUID paymentId, CancelReason reason) {
}
