package com.sagademo.saga.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * THE FULL, REAL EXECUTION HISTORY of one distributed transaction -
 * the answer to {@code GET /api/sagas/{sagaId}} (requirement #7).
 *
 * <p>Everything in here is read from the Saga database, which was written while the
 * orchestrator was really calling the other microservices over HTTP. The frontend renders it
 * and animates it; it never invents it.
 */
public record SagaDetailResponse(
        boolean success,
        String sagaId,
        String orderId,
        String status,
        String currentStep,
        String customerName,
        String productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String reservationId,
        String paymentId,
        String failedStep,
        String failureReason,
        boolean compensationRequired,
        boolean manualRecoveryRequired,
        String recoveryHint,
        Instant startedAt,
        Instant completedAt,
        long durationMs,
        boolean finished,

        /** The timeline: forward steps first, then compensation steps, in execution order. */
        List<SagaStepDto> steps,

        /** Every HTTP call between services, in order. */
        List<ServiceCallDto> serviceCalls,

        /** The human-readable log lines. */
        List<SagaLogDto> logs,

        /** System state captured just before the saga started. */
        SystemStateDto stateBefore,

        /** System state captured when the saga reached an end state. */
        SystemStateDto stateAfter,

        /** Live system state, read at the moment this response was built. */
        SystemStateDto stateNow
) {
}
