package com.sagademo.saga.client.dto;

/** Payload for {@code POST /api/orders/{id}/confirm} and {@code .../cancel}. */
public record OrderActionPayload(
        String sagaId,
        String reason,
        boolean simulateFailure
) {
}
