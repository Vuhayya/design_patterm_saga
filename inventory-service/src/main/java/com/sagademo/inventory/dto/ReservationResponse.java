package com.sagademo.inventory.dto;

import com.sagademo.inventory.entity.InventoryItem;
import com.sagademo.inventory.entity.InventoryReservation;

import java.time.Instant;

/**
 * Result of RESERVE_INVENTORY / RELEASE_INVENTORY.
 *
 * <p>It deliberately echoes the resulting stock numbers so the frontend can show the
 * "99 units reserved" -> "100 units" transition without a second round trip.
 */
public record ReservationResponse(
        boolean success,
        String service,
        String reservationId,
        String sagaId,
        String orderId,
        String productId,
        String productName,
        Integer quantity,
        String status,
        Integer totalUnits,
        Integer reservedUnits,
        Integer availableUnits,
        Instant reservedAt,
        Instant releasedAt
) {

    public static ReservationResponse of(InventoryReservation reservation, InventoryItem item) {
        return new ReservationResponse(
                true,
                "inventory-service",
                reservation.getReservationId(),
                reservation.getSagaId(),
                reservation.getOrderId(),
                reservation.getProductId(),
                item.getProductName(),
                reservation.getQuantity(),
                reservation.getStatus().name(),
                item.getTotalUnits(),
                item.getReservedUnits(),
                item.getAvailableUnits(),
                reservation.getReservedAt(),
                reservation.getReleasedAt()
        );
    }
}
