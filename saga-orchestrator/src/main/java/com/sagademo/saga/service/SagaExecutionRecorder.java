package com.sagademo.saga.service;

import com.sagademo.saga.dto.StartSagaRequest;
import com.sagademo.saga.dto.SystemStateDto;
import com.sagademo.saga.entity.SagaExecution;
import com.sagademo.saga.entity.SagaStepName;
import com.sagademo.saga.saga.SagaStep;
import java.util.List;

/**
 * ALL WRITES to the Saga database live here, so that {@code SagaOrchestratorService} contains nothing but the Saga algorithm itself.
 *
 * <p>Contract only. Controllers and other services depend on this interface, never on the
 * implementation class.
 *
 * @see com.sagademo.saga.service.impl.SagaExecutionRecorderImpl
 */
public interface SagaExecutionRecorder {

    /** Creates the saga row in state PENDING and pre-creates the four forward steps in state PENDING, so the frontend can draw the whole plan before anything has happened yet. */
    SagaExecution createSaga(StartSagaRequest request, List<SagaStep> steps, SystemStateDto stateBefore);

    void markRunning(String sagaId);

    void markCompleted(String sagaId);

    /** A forward step failed. */
    void markFailed(String sagaId, SagaStepName failedStep, String reason);

    void markCompensating(String sagaId);

    void markCompensated(String sagaId);

    /** The honest end state: at least one compensating transaction failed, so the system is inconsistent and a human has to fix it (requirement #28). */
    void markCompensationFailed(String sagaId, String recoveryHint);

    void updateCurrentStep(String sagaId, SagaStepName step);

    /** Stores the ids of the resources the saga created in the other services. */
    void updateCorrelationIds(String sagaId, String orderId, String reservationId, String paymentId);

    void saveStateAfter(String sagaId, SystemStateDto stateAfter);

    void forwardStepStarted(String sagaId, SagaStep step);

    void forwardStepCompleted(String sagaId, SagaStep step);

    void forwardStepFailed(String sagaId, SagaStep step, String errorMessage);

    /** Marks the forward steps that were never reached, so the timeline shows them greyed out. */
    void markRemainingStepsSkipped(String sagaId);

    /** Creates the COMPENSATION row for a completed forward step. */
    void compensationStarted(String sagaId, SagaStep step);

    void compensationCompleted(String sagaId, SagaStep step);

    void compensationFailed(String sagaId, SagaStep step, String errorMessage);

    void log(String sagaId, String level, String phase, String message);
}
