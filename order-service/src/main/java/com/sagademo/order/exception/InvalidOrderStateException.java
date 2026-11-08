package com.sagademo.order.exception;

/**
 * The requested transition is not legal for the current order state, e.g. confirming an
 * order that was already cancelled. Returned as HTTP 409 so the orchestrator can tell
 * "business rule violation" apart from "service is down".
 */
public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
