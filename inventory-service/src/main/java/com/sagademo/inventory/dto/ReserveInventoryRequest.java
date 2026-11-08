package com.sagademo.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload the Saga Orchestrator sends for Saga step 2 (RESERVE_INVENTORY). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveInventoryRequest {

    @NotBlank(message = "sagaId is required")
    private String sagaId;

    @NotBlank(message = "orderId is required")
    private String orderId;

    @NotBlank(message = "productId is required")
    private String productId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    /**
     * FAILURE SIMULATION: when true the reservation really throws and nothing is committed,
     * which lets the presentation demonstrate demo Scenario 3 (inventory failure).
     */
    private boolean simulateFailure;
}
