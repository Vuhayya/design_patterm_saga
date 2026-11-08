package com.sagademo.saga.entity;

/**
 * Status of ONE step of the Saga. This is what paints every box in the frontend timeline.
 *
 * <pre>
 *   Forward step:        PENDING -> RUNNING -> COMPLETED | FAILED | SKIPPED
 *   Compensation step:   PENDING -> RUNNING -> COMPENSATED | COMPENSATION_FAILED
 * </pre>
 */
public enum StepStatus {

    /** Not reached yet. */
    PENDING,

    /** In flight - the HTTP call to the owning microservice is happening right now. */
    RUNNING,

    /** The forward local transaction committed successfully. */
    COMPLETED,

    /** The forward local transaction failed. This is what triggers compensation. */
    FAILED,

    /** The compensating transaction committed successfully - the step is undone. */
    COMPENSATED,

    /** The compensating transaction failed. Manual recovery needed. */
    COMPENSATION_FAILED,

    /** Never executed, because the Saga failed before reaching this step. */
    SKIPPED
}
