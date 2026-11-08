package com.sagademo.saga.entity;

import java.util.Arrays;

/**
 * THE COMPENSATION MAP - the single place that answers the question
 * "which compensating transaction undoes which local transaction?".
 *
 * <p>Requirement #4 says the system must know exactly which compensation belongs to each
 * completed step. Putting that knowledge in one enum (instead of scattering {@code if}
 * statements through the orchestrator) is what makes it explainable in one slide:
 *
 * <pre>
 *   CREATE_ORDER       ->  CANCEL_ORDER        (order-service)
 *   RESERVE_INVENTORY  ->  RELEASE_INVENTORY   (inventory-service)
 *   PROCESS_PAYMENT    ->  REFUND_PAYMENT      (payment-service)
 *   CONFIRM_ORDER      ->  NONE
 * </pre>
 *
 * <p>Why does CONFIRM_ORDER have no compensation? Because it is the last forward step. If it
 * fails it did not commit anything of its own, so there is nothing of ITS OWN to undo - what
 * has to be undone are the three steps before it. If CONFIRM_ORDER had succeeded, the whole
 * Saga would have succeeded and no compensation would ever run.
 */
public enum CompensationAction {

    CANCEL_ORDER(SagaStepName.CREATE_ORDER, SagaStepName.CANCEL_ORDER, "order-service"),
    RELEASE_INVENTORY(SagaStepName.RESERVE_INVENTORY, SagaStepName.RELEASE_INVENTORY, "inventory-service"),
    REFUND_PAYMENT(SagaStepName.PROCESS_PAYMENT, SagaStepName.REFUND_PAYMENT, "payment-service"),
    NONE(SagaStepName.CONFIRM_ORDER, null, null);

    private final SagaStepName forwardStep;
    private final SagaStepName compensationStep;
    private final String targetService;

    CompensationAction(SagaStepName forwardStep, SagaStepName compensationStep, String targetService) {
        this.forwardStep = forwardStep;
        this.compensationStep = compensationStep;
        this.targetService = targetService;
    }

    public SagaStepName forwardStep() {
        return forwardStep;
    }

    /** The compensating transaction, or {@code null} when the forward step needs none. */
    public SagaStepName compensationStep() {
        return compensationStep;
    }

    public String targetService() {
        return targetService;
    }

    public boolean hasCompensation() {
        return compensationStep != null;
    }

    /** Looks up the compensation that belongs to a completed forward step. */
    public static CompensationAction forStep(SagaStepName forwardStep) {
        return Arrays.stream(values())
                .filter(action -> action.forwardStep == forwardStep)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No compensation mapping defined for step " + forwardStep));
    }
}
