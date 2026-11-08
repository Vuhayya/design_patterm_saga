package com.sagademo.saga.client.dto;

import java.math.BigDecimal;

/** Payload for {@code POST /api/payments/process}. */
public record ProcessPaymentPayload(
        String sagaId,
        String orderId,
        String customerName,
        BigDecimal amount,
        boolean simulateFailure
) {
}
