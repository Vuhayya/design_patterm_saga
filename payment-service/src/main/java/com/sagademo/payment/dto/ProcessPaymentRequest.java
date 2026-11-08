package com.sagademo.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Payload the Saga Orchestrator sends for Saga step 3 (PROCESS_PAYMENT). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequest {

    @NotBlank(message = "sagaId is required")
    private String sagaId;

    @NotBlank(message = "orderId is required")
    private String orderId;

    private String customerName;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than 0")
    private BigDecimal amount;

    /**
     * FAILURE SIMULATION: this is the switch used by the headline demo (Scenario 2).
     * The payment genuinely fails, a FAILED row is written, HTTP 500 is returned, and the
     * orchestrator genuinely compensates the two earlier steps.
     */
    private boolean simulateFailure;
}
