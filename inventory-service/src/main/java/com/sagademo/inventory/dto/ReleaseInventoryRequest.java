package com.sagademo.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for the COMPENSATING transaction C2 (RELEASE_INVENTORY).
 *
 * <p>Either {@code reservationId} or {@code sagaId} is enough to locate what has to be undone.
 * Being able to compensate from the correlation id alone is important: a real orchestrator may
 * have crashed and restarted before it got the chance to store the reservation id.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseInventoryRequest {

    @NotBlank(message = "sagaId is required")
    private String sagaId;

    /** Optional - if absent, the reservation is resolved by sagaId. */
    private String reservationId;

    private String reason;

    /** FAILURE SIMULATION for the COMPENSATION itself (demo Scenario 5). */
    private boolean simulateFailure;
}
