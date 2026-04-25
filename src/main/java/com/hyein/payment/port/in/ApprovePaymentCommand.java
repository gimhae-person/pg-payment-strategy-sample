package com.hyein.payment.port.in;

import com.hyein.payment.port.out.PgAuthResult;

import java.util.UUID;

public record ApprovePaymentCommand(UUID paymentId, PgAuthResult authResult) {
}
