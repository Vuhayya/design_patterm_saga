package com.sagademo.inventory.dto;

import java.time.Instant;

/**
 * Uniform error envelope (same shape in every service of this demo) so the Saga
 * Orchestrator can always extract a meaningful failure reason.
 */
public record ApiErrorResponse(
        boolean success,
        String service,
        String errorCode,
        String message,
        String sagaId,
        Instant timestamp
) {

    public static ApiErrorResponse of(String errorCode, String message, String sagaId) {
        return new ApiErrorResponse(false, "inventory-service", errorCode, message, sagaId, Instant.now());
    }
}
