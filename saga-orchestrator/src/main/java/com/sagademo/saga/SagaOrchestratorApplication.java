package com.sagademo.saga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * SAGA ORCHESTRATOR - independent Spring Boot microservice (port 8084).
 *
 * <p>This is the heart of the project. It owns no business data at all: it owns the
 * <b>state of the distributed transaction</b> in its own Saga database
 * ({@code saga_executions}, {@code saga_step_executions}, {@code service_call_logs},
 * {@code saga_log_entries}).
 *
 * <p>It implements ORCHESTRATION-based Saga: one central component decides what happens next
 * and, on failure, what has to be undone. The alternative style (CHOREOGRAPHY, where each
 * service reacts to events) is explained on the "How Saga Works" page of the frontend and in
 * {@code docs/KAFKA_ROADMAP.md}.
 *
 * <p>{@code @EnableAsync} is what lets {@code POST /api/sagas/orders} return the sagaId
 * immediately while the saga keeps running in the background - that is what makes the
 * frontend able to animate the steps one by one as they really happen.
 */
@EnableAsync
@SpringBootApplication
public class SagaOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SagaOrchestratorApplication.class, args);
    }
}
