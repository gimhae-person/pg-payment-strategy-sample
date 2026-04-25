package com.hyein.payment.adapter.pg.toss;

public record TossProperties(
        String clientKey,
        String checkoutUrl,
        String approveUrl,
        String cancelUrl
) {
}
