package com.sagademo.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for the COMPENSATING transaction C3 (REFUND_PAYMENT).
 * Either {@code paymentId} or {@code sagaId} is enough to find the charge to reverse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundPaymentRequest {

    @NotBlank(message = "sagaId is required")
    private String sagaId;

    /** Optional - if absent the payment is resolved by sagaId. */
    private String paymentId;

    private String reason;

    /** FAILURE SIMULATION for the COMPENSATION itself (demo Scenario 5). */
    private boolean simulateFailure;
}
