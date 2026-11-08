package com.sagademo.saga.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Stock snapshot read from inventory-service for the STATE COMPARISON panel. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryItemClientResponse(
        String productId,
        String productName,
        Integer totalUnits,
        Integer reservedUnits,
        Integer availableUnits,
        BigDecimal unitPrice
) {
}
