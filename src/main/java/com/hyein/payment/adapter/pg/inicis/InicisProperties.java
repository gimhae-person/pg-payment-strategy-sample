package com.hyein.payment.adapter.pg.inicis;

public record InicisProperties(
        String mid,
        String signKey,
        String checkoutUrl,
        String approveUrl,
        String cancelUrl,
        String netCancelUrl
) {
}
