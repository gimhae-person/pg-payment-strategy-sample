package com.hyein.payment.port.in;

import com.hyein.payment.domain.Money;
import com.hyein.payment.domain.PaymentMethod;
import com.hyein.payment.domain.PgCompany;

public record CreatePaymentCommand(
        String orderId,
        PgCompany pgCompany,
        PaymentMethod method,
        Money amount
) {
}
