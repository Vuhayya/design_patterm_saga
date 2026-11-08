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
 * ONE STEP of one distributed transaction - the row behind every box in the frontend timeline.
 *
 * <p>Both phases are recorded in the same table, told apart by {@link #stepType}:
 * <ul>
 *   <li>{@code FORWARD} rows: CREATE_ORDER, RESERVE_INVENTORY, PROCESS_PAYMENT, CONFIRM_ORDER</li>
 *   <li>{@code COMPENSATION} rows: RELEASE_INVENTORY, REFUND_PAYMENT, CANCEL_ORDER</li>
 * </ul>
 *
 * <p>{@link #compensationFor} makes the link explicit, so the UI can literally draw
 * "RELEASE_INVENTORY undoes RESERVE_INVENTORY".
 */
@Entity
@Table(name = "saga_step_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaStepExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "saga_id", nullable = false, length = 60)
    private String sagaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_name", nullable = false, length = 40)
    private SagaStepName stepName;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 20)
    private StepType stepType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StepStatus status;

    /** Which microservice owns this local transaction (order/inventory/payment-service). */
    @Column(name = "target_service", length = 40)
    private String targetService;

    /** For COMPENSATION rows: the FORWARD step this row undoes. */
    @Enumerated(EnumType.STRING)
    @Column(name = "compensation_for", length = 40)
    private SagaStepName compensationFor;

    /** Execution order within the saga - keeps the timeline stable. */
    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** The error envelope message returned by the failing service. Rendered in red in the UI. */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /** How many attempts this step needed (compensations may be retried). */
    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    public long getDurationMs() {
        if (startedAt == null) {
            return 0;
        }
        Instant end = completedAt == null ? Instant.now() : completedAt;
        return end.toEpochMilli() - startedAt.toEpochMilli();
    }
}
