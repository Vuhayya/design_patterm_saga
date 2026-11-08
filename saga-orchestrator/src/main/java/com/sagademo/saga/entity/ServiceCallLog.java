package com.sagademo.saga.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * ONE REAL HTTP CALL between two microservices.
 *
 * <p>This table is the data source of the "SERVICE COMMUNICATION" panel (requirement #11).
 * Every row is recorded by {@code SagaHttpClient} around an actual network round trip, so
 * what the lecturer sees on screen is a log of traffic that really happened:
 *
 * <pre>
 *   POST http://localhost:8082/api/inventory/reserve   200 OK      118 ms
 *   POST http://localhost:8083/api/payments/process    500 ERROR   271 ms
 *   POST http://localhost:8082/api/inventory/release   200 OK       96 ms
 *   POST http://localhost:8081/api/orders/ORDER-7/cancel 200 OK     87 ms
 * </pre>
 */
@Entity
@Table(name = "service_call_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "saga_id", nullable = false, length = 60)
    private String sagaId;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    /** Which saga step this call belongs to. */
    @Enumerated(EnumType.STRING)
    @Column(name = "step_name", length = 40)
    private SagaStepName stepName;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", length = 20)
    private StepType stepType;

    /** Always "saga-orchestrator" in this project - the caller. */
    @Column(name = "source_service", length = 40)
    private String sourceService;

    /** The callee, e.g. "payment-service". */
    @Column(name = "target_service", nullable = false, length = 40)
    private String targetService;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "endpoint", nullable = false, length = 300)
    private String endpoint;

    /** Null when the call never reached the server (e.g. the service is down). */
    @Column(name = "response_status")
    private Integer responseStatus;

    /** SUCCESS or ERROR - drives the green/red colouring in the UI. */
    @Column(name = "outcome", nullable = false, length = 20)
    private String outcome;

    @Column(name = "request_summary", length = 1000)
    private String requestSummary;

    @Column(name = "response_summary", length = 1000)
    private String responseSummary;

    @Column(name = "called_at", nullable = false)
    private Instant calledAt;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;
}
