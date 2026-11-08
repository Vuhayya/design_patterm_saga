package com.sagademo.saga.event;

/**
 * THE KAFKA SEAM (requirement: "structure the code so that an event-driven Kafka
 * implementation could be added later").
 *
 * <p>The orchestrator publishes an event at every meaningful point of the Saga through this one
 * interface. Version 1 of the project ships {@link LoggingSagaEventPublisher}, which only writes
 * the events to the console - no broker, no extra infrastructure to install before a presentation.
 *
 * <p>To move to an event-driven implementation you add a second implementation:
 *
 * <pre>
 * &#64;Component
 * &#64;Primary
 * class KafkaSagaEventPublisher implements SagaEventPublisher {
 *     private final KafkaTemplate&lt;String, SagaEvent&gt; kafkaTemplate;
 *
 *     public void publish(SagaEvent event) {
 *         // the sagaId as the message key keeps one saga's events in one partition,
 *         // which is what preserves their order
 *         kafkaTemplate.send("saga-events", event.sagaId(), event);
 *     }
 * }
 * </pre>
 *
 * Nothing in {@code SagaOrchestratorService} changes.
 */
public interface SagaEventPublisher {

    void publish(SagaEvent event);
}
