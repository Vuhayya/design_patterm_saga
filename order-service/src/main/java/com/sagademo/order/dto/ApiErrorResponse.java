package com.sagademo.order.dto;

import java.time.Instant;

/**
 * Uniform error envelope returned by every service in this demo.
 *
 * <p>The Saga Orchestrator parses this to write a meaningful {@code errorMessage} on the
 * failed {@code SagaStepExecution}, which is exactly what the frontend renders in red.
 *
 * <pre>
 * {
 *   "success": false,
 *   "service": "order-service",
 *   "errorCode": "ORDER_CONFIRMATION_FAILED",
 *   "message": "Simulated failure while confirming order ORDER-0007",
 *   "timestamp": "2026-09-26T10:31:04.221Z"
 * }
 * </pre>
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
        return new ApiErrorResponse(false, "order-service", errorCode, message, sagaId, Instant.now());
    }
}
