package com.sagademo.saga.exception;

/** No saga with that id exists in the Saga database. */
public class SagaNotFoundException extends RuntimeException {

    public SagaNotFoundException(String sagaId) {
        super("Saga not found: " + sagaId);
    }
}
