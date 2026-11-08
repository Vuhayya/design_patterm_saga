package com.sagademo.saga.client.dto;

/** Payload for the compensating call {@code POST /api/inventory/release}. */
public record ReleaseInventoryPayload(
        String sagaId,
        String reservationId,
        String reason,
        boolean simulateFailure
) {
}
