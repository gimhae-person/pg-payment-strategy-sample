package com.hyein.payment.adapter.pg.kcp;

public record KcpProperties(
        String siteCode,
        String checkoutUrl,
        String approveUrl,
        String cancelUrl
) {
}
