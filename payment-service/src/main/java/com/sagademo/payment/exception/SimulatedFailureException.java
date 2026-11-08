package com.sagademo.payment.exception;

import lombok.Getter;

/** Thrown when the demo operator asked the Payment Service to fail. Real exception, real HTTP 500. */
@Getter
public class SimulatedFailureException extends RuntimeException {

    private final String errorCode;
    private final String sagaId;

    public SimulatedFailureException(String errorCode, String message, String sagaId) {
        super(message);
        this.errorCode = errorCode;
        this.sagaId = sagaId;
    }
}
