package com.sagademo.saga.event;

import com.sagademo.saga.entity.SagaStepName;

import java.time.Instant;

/**
 * A domain event describing something that happened to a Saga.
 *
 * <p>Today these events are only logged. They exist so that the project can be extended to an
 * EVENT-DRIVEN (choreography) implementation without touching the Saga logic: publishing the
 * same events to Kafka topics is all that changes. See {@code docs/KAFKA_ROADMAP.md}.
 */
public record SagaEvent(
        String eventType,
        String sagaId,
        String orderId,
        SagaStepName step,
        String status,
        String message,
        Instant occurredAt
) {

    public static SagaEvent of(String eventType, String sagaId, String orderId,
                               SagaStepName step, String status, String message) {
        return new SagaEvent(eventType, sagaId, orderId, step, status, message, Instant.now());
    }
}
