package com.sagademo.saga.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** What inventory-service answers for reserve/release. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservationClientResponse(
        String reservationId,
        String sagaId,
        String orderId,
        String productId,
        String productName,
        Integer quantity,
        String status,
        Integer totalUnits,
        Integer reservedUnits,
        Integer availableUnits
) {
}
