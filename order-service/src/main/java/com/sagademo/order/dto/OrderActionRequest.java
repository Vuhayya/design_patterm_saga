package com.sagademo.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for the two follow-up operations on an existing order:
 * CONFIRM_ORDER (forward step 4) and CANCEL_ORDER (compensation for step 1).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderActionRequest {

    @NotBlank(message = "sagaId is required")
    private String sagaId;

    /** Why the order is being cancelled - shown in the demo UI and stored for audit. */
    private String reason;

    /** See {@link CreateOrderRequest#isSimulateFailure()}. */
    private boolean simulateFailure;
}
