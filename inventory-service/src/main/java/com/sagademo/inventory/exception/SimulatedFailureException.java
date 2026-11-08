package com.sagademo.inventory.exception;

import lombok.Getter;

/**
 * Thrown when the demo operator asked the Inventory Service to fail.
 * Real exception, real HTTP 500, real rollback of this one local transaction.
 */
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
