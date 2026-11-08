package com.sagademo.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload the Saga Orchestrator sends for Saga step 1 (CREATE_ORDER).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    /** Correlation id of the distributed transaction. Required so this service can log/trace it. */
    @NotBlank(message = "sagaId is required")
    private String sagaId;

    @NotBlank(message = "customerName is required")
    private String customerName;

    @NotBlank(message = "productId is required")
    private String productId;

    private String productName;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "unitPrice is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "unitPrice must be greater than 0")
    private BigDecimal unitPrice;

    /**
     * FAILURE SIMULATION (per request).
     * When true this service really throws, really returns HTTP 500 and really does
     * not commit anything. The Saga Orchestrator then really runs compensation.
     * Nothing about the failure is faked in the frontend.
     */
    private boolean simulateFailure;
}
