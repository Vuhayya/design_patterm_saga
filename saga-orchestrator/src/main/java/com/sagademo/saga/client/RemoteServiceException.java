package com.sagademo.saga.client;

import lombok.Getter;

/**
 * A remote microservice refused to perform its local transaction (or could not be reached).
 *
 * <p>This is the exception the orchestrator catches to decide "this step failed, start
 * compensation". It carries the information the UI needs to explain WHY:
 *
 * <pre>
 *   service     = payment-service
 *   httpStatus  = 500              (0 when the service could not be reached at all)
 *   errorCode   = PAYMENT_FAILED
 *   message     = Simulated failure: the payment gateway declined the charge ...
 * </pre>
 */
@Getter
public class RemoteServiceException extends RuntimeException {

    private final String service;
    private final int httpStatus;
    private final String errorCode;

    public RemoteServiceException(String service, int httpStatus, String errorCode, String message) {
        super(message);
        this.service = service;
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    /** The service process is not reachable (stopped, crashed, wrong port). */
    public static RemoteServiceException unreachable(String service, String endpoint, String detail) {
        return new RemoteServiceException(service, 0, "SERVICE_UNAVAILABLE",
                "%s could not be reached at %s (%s)".formatted(service, endpoint, detail));
    }

    /** Human readable one-liner used in the Saga step's errorMessage column. */
    public String describe() {
        return "[%s] %s (HTTP %s): %s".formatted(service, errorCode, httpStatus, getMessage());
    }
}
