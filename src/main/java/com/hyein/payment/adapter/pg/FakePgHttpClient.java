package com.hyein.payment.adapter.pg;

import java.util.Map;
import java.util.UUID;

public final class FakePgHttpClient implements PgHttpClient {
    @Override
    public PgHttpResponse postForm(String url, Map<String, String> form) {
        String prefix = form.getOrDefault("pg", "PG");
        String tid = prefix + "-" + UUID.randomUUID();
        return new PgHttpResponse(true, tid, "OK:" + url, Map.of("status", "APPROVED"));
    }
}
