package com.sagademo.payment.dto;

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
        return new ApiErrorResponse(false, "payment-service", errorCode, message, sagaId, Instant.now());
    }
}
