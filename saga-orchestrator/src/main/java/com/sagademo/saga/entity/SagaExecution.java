package com.sagademo.saga.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * ONE DISTRIBUTED TRANSACTION.
 *
 * <p>This is the orchestrator's own aggregate, stored in the Saga database. It is the
 * durable "memory" of the Saga: if the orchestrator is restarted you can still look at this
 * row and know exactly how far the transaction got and what was compensated.
 *
 * <p>Requirement #4 asks for {@code sagaId, orderId, status, currentStep, startedAt,
 * completedAt}; the extra columns exist to drive the demo UI (the business payload, the ids
 * of the resources created in the other services, and the before/after state snapshots).
 */
@Entity
@Table(name = "saga_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaExecution {

    /** Correlation id of the whole distributed transaction, e.g. {@code SAGA-0001}. */
    @Id
    @Column(name = "saga_id", nullable = false, length = 60)
    private String sagaId;

    /** Filled in as soon as CREATE_ORDER succeeds. Null while the saga is still on step 1. */
    @Column(name = "order_id", length = 40)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SagaStatus status;

    /** The step the orchestrator is working on right now (or the last one it worked on). */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", length = 40)
    private SagaStepName currentStep;

    // ---- business payload of the transaction ------------------------------------------
    @Column(name = "customer_name", length = 120)
    private String customerName;

    @Column(name = "product_id", length = 60)
    private String productId;

    @Column(name = "product_name", length = 120)
    private String productName;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // ---- ids of the resources this saga created in OTHER services ---------------------
    /** Reservation created in inventory-service (needed to compensate it). */
    @Column(name = "reservation_id", length = 40)
    private String reservationId;

    /** Payment created in payment-service (needed to refund it). */
    @Column(name = "payment_id", length = 40)
    private String paymentId;

    // ---- failure / compensation bookkeeping -------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "failed_step", length = 40)
    private SagaStepName failedStep;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "compensation_required", nullable = false)
    private boolean compensationRequired;

    /** True when a compensating transaction failed - a human must fix the data by hand. */
    @Column(name = "manual_recovery_required", nullable = false)
    private boolean manualRecoveryRequired;

    @Column(name = "recovery_hint", length = 1000)
    private String recoveryHint;

    // ---- timing -----------------------------------------------------------------------
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    // ---- state snapshots that power the BEFORE / AFTER comparison panel ---------------
    @Lob
    @Column(name = "state_before_json")
    private String stateBeforeJson;

    @Lob
    @Column(name = "state_after_json")
    private String stateAfterJson;

    @Column(name = "sequence_no", nullable = false)
    private Long sequenceNo;

    /** True once the saga reached one of its end states. */
    public boolean isFinished() {
        return status == SagaStatus.COMPLETED
                || status == SagaStatus.COMPENSATED
                || status == SagaStatus.COMPENSATION_FAILED;
    }

    public long getDurationMs() {
        Instant end = completedAt == null ? Instant.now() : completedAt;
        return end.toEpochMilli() - startedAt.toEpochMilli();
    }
}
