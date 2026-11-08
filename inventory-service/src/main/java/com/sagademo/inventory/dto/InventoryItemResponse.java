package com.sagademo.inventory.dto;

import com.sagademo.inventory.entity.InventoryItem;

import java.math.BigDecimal;

/** Stock snapshot of one product - feeds the "STATE COMPARISON" panel of the demo UI. */
public record InventoryItemResponse(
        String productId,
        String productName,
        Integer totalUnits,
        Integer reservedUnits,
        Integer availableUnits,
        BigDecimal unitPrice
) {

    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getTotalUnits(),
                item.getReservedUnits(),
                item.getAvailableUnits(),
                item.getUnitPrice()
        );
    }
}
