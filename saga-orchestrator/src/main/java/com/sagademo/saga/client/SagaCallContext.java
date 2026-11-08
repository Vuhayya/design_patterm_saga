package com.sagademo.saga.client;

import com.sagademo.saga.entity.SagaStepName;
import com.sagademo.saga.entity.StepType;

/**
 * The tracing context attached to one inter-service HTTP call: which saga, which order,
 * which step, and whether it is a forward step or a compensation.
 *
 * <p>It is what turns a plain HTTP call into a traceable piece of a distributed transaction -
 * it becomes the {@code X-Saga-Id} / {@code X-Order-Id} / {@code X-Saga-Step} headers and the
 * {@code ServiceCallLog} row.
 */
public record SagaCallContext(
        String sagaId,
        String orderId,
        SagaStepName stepName,
        StepType stepType
) {

    public static SagaCallContext forward(String sagaId, String orderId, SagaStepName stepName) {
        return new SagaCallContext(sagaId, orderId, stepName, StepType.FORWARD);
    }

    public static SagaCallContext compensation(String sagaId, String orderId, SagaStepName stepName) {
        return new SagaCallContext(sagaId, orderId, stepName, StepType.COMPENSATION);
    }
}
