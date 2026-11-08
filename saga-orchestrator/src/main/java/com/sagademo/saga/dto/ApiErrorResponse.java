package com.sagademo.saga.dto;

import java.time.Instant;

/** Uniform error envelope (identical shape in every service of this demo). */
public record ApiErrorResponse(
        boolean success,
        String service,
        String errorCode,
        String message,
        String sagaId,
        Instant timestamp
) {

    public static ApiErrorResponse of(String errorCode, String message, String sagaId) {
        return new ApiErrorResponse(false, "saga-orchestrator", errorCode, message, sagaId, Instant.now());
    }
}
