package com.sagademo.order.exception;

import lombok.Getter;

/**
 * Thrown when the demo operator asked this service to fail.
 *
 * <p>This is a REAL exception on a REAL HTTP request. Because it is thrown before/inside
 * the {@code @Transactional} method completes, the local transaction is rolled back by the
 * database and nothing is committed here - which is precisely the situation the Saga
 * Orchestrator has to react to by compensating the *earlier, already committed* steps in
 * the *other* services.
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
