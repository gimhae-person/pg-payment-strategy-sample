package com.hyein.payment.port.in;

import com.hyein.payment.domain.PgCompany;

import java.util.Map;

public record HandleWebhookCommand(
        PgCompany pgCompany,
        Map<String, String> headers,
        Map<String, String> body
) {
    public HandleWebhookCommand {
        headers = Map.copyOf(headers);
        body = Map.copyOf(body);
    }
}
