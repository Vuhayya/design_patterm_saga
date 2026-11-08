package com.sagademo.saga.dto;

import java.time.Instant;

/**
 * A SNAPSHOT OF THE WHOLE SYSTEM at one moment in time, assembled by querying all three
 * business services. This is the data behind the STATE COMPARISON panel (requirement #13):
 *
 * <pre>
 *   BEFORE SAGA            DURING EXECUTION          AFTER COMPENSATION
 *   Inventory: 100 units   Inventory: 99 (1 held)    Inventory: 100 units
 *   Payment:   NOT_PROCESSED  Payment: PROCESSING    Payment:   REFUNDED / FAILED
 *   Order:     NOT_CREATED    Order:   CREATED       Order:     CANCELLED
 * </pre>
 *
 * <p>It is what makes the *purpose* of compensation visible: the numbers really go back.
 */
public record SystemStateDto(
        String label,
        String productId,
        String productName,
        Integer inventoryTotalUnits,
        Integer inventoryReservedUnits,
        Integer inventoryAvailableUnits,
        String paymentStatus,
        String paymentReference,
        String orderStatus,
        Instant capturedAt
) {

    public static final String NOT_CREATED = "NOT_CREATED";
    public static final String NOT_PROCESSED = "NOT_PROCESSED";
    public static final String UNKNOWN = "UNKNOWN";
}
