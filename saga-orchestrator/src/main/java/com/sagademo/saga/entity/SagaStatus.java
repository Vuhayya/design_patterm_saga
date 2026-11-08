package com.sagademo.saga.entity;

/**
 * EXPLICIT SAGA STATE MACHINE.
 *
 * <p>Happy path:
 * <pre>
 *   PENDING -> RUNNING -> COMPLETED
 * </pre>
 *
 * <p>Failure path:
 * <pre>
 *   PENDING -> RUNNING -> FAILED -> COMPENSATING -> COMPENSATED
 *                                              \
 *                                               -> COMPENSATION_FAILED   (requires manual recovery)
 * </pre>
 *
 * <p>Notice that FAILED is NOT the end state. A Saga that merely "failed" would leave the
 * system inconsistent; the orchestrator must drive it to COMPENSATED. The only truly bad
 * outcome is COMPENSATION_FAILED, and the honest answer for that case is "the Saga pattern
 * does not magically guarantee rollback" - see requirement #28 and the UI banner.
 */
public enum SagaStatus {

    /** Created in the Saga database, not started yet. */
    PENDING,

    /** Forward steps are executing. */
    RUNNING,

    /** All forward steps committed. The business transaction succeeded. */
    COMPLETED,

    /** A forward step failed. Transient state - compensation is about to start. */
    FAILED,

    /** Compensating transactions are running in reverse order. */
    COMPENSATING,

    /** Every required compensation succeeded. The system is consistent again. */
    COMPENSATED,

    /**
     * At least one compensating transaction failed even after retries.
     * The system is left inconsistent and a human has to intervene.
     */
    COMPENSATION_FAILED
}
