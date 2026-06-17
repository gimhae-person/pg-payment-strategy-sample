package com.hyein.payment.port.in;

import com.hyein.payment.port.out.PgApprovalPayload;

import java.util.UUID;

public record ApprovePaymentCommand(UUID paymentId, PgApprovalPayload approvalPayload) {
}
