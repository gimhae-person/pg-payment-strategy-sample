package com.hyein.payment.application;

import com.hyein.payment.domain.PgCompany;
import com.hyein.payment.port.out.PaymentGateway;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PaymentGatewayRegistry {
    private final Map<PgCompany, PaymentGateway> gateways;

    public PaymentGatewayRegistry(List<PaymentGateway> gateways) {
        this.gateways = new EnumMap<>(PgCompany.class);
        for (PaymentGateway gateway : gateways) {
            PaymentGateway previous = this.gateways.putIfAbsent(gateway.company(), gateway);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicated payment gateway: " + gateway.company());
            }
        }
    }

    public PaymentGateway get(PgCompany company) {
        PaymentGateway gateway = gateways.get(company);
        if (gateway == null) {
            throw new IllegalArgumentException("Unsupported PG company: " + company);
        }
        return gateway;
    }
}
