package com.sagademo.saga.client.dto;

/** Payload for {@code POST /api/inventory/reserve}. */
public record ReserveInventoryPayload(
        String sagaId,
        String orderId,
        String productId,
        Integer quantity,
        boolean simulateFailure
) {
}
