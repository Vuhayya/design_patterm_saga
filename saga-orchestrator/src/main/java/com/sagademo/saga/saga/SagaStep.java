package com.sagademo.saga.saga;

import com.sagademo.saga.entity.CompensationAction;
import com.sagademo.saga.entity.SagaStepName;

/**
 * ONE STEP OF THE SAGA - the central abstraction of this project.
 *
 * <p>Every step knows two things, and that is the entire Saga pattern in one interface:
 * <ol>
 *   <li>{@link #execute(SagaContext)}    - how to DO its local transaction</li>
 *   <li>{@link #compensate(SagaContext)} - how to UNDO it semantically, if a LATER step fails</li>
 * </ol>
 *
 * <p>Because the pair lives together in one class, the orchestrator never has to guess how to
 * roll something back: it keeps a stack of the steps that succeeded and calls
 * {@code compensate()} on them in reverse order.
 *
 * <pre>
 *   forward :  CREATE_ORDER -> RESERVE_INVENTORY -> PROCESS_PAYMENT -> CONFIRM_ORDER
 *   undo    :  CANCEL_ORDER &lt;- RELEASE_INVENTORY &lt;- REFUND_PAYMENT
 * </pre>
 *
 * <p>Adding a fifth step to the business transaction (say NOTIFY_RESTAURANT) means writing one
 * more class that implements this interface and registering it in {@code SagaDefinition} - the
 * orchestrator itself does not change. That is the extensibility point to mention when someone
 * asks how this would grow.
 */
public interface SagaStep {

    /** Which step this is, e.g. {@code RESERVE_INVENTORY}. */
    SagaStepName name();

    /** Which microservice owns the local transaction, e.g. {@code inventory-service}. */
    String targetService();

    /**
     * Runs the forward LOCAL TRANSACTION by calling the owning microservice over HTTP.
     *
     * @throws com.sagademo.saga.client.RemoteServiceException when the service refuses or is unreachable;
     *         this is the signal the orchestrator uses to start compensation
     */
    void execute(SagaContext context);

    /**
     * Runs the COMPENSATING TRANSACTION that semantically undoes {@link #execute}.
     *
     * <p>Must be idempotent - the orchestrator is allowed to retry it.
     *
     * @throws com.sagademo.saga.client.RemoteServiceException when the compensation itself fails,
     *         which leads to SAGA COMPENSATION FAILED (requirement #28)
     */
    void compensate(SagaContext context);

    /** The compensation mapping for this step, taken from the single source of truth. */
    default CompensationAction compensationAction() {
        return CompensationAction.forStep(name());
    }

    /** The name of the compensating transaction, or {@code null} if this step needs none. */
    default SagaStepName compensationName() {
        return compensationAction().compensationStep();
    }

    /** False only for the last forward step, which has nothing of its own to undo. */
    default boolean hasCompensation() {
        return compensationAction().hasCompensation();
    }
}
