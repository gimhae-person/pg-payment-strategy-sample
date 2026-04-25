package com.hyein.payment.adapter.pg;

import java.util.Map;

public interface PgHttpClient {
    PgHttpResponse postForm(String url, Map<String, String> form);
}
