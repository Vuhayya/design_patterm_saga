package com.sagademo.saga.client.dto;

/** Payload for the compensating call {@code POST /api/payments/refund}. */
public record RefundPaymentPayload(
        String sagaId,
        String paymentId,
        String reason,
        boolean simulateFailure
) {
}
