package com.sagademo.saga.dto;

import com.sagademo.saga.entity.SagaExecution;

import java.math.BigDecimal;
import java.time.Instant;

/** Compact row for the "recent sagas" history list on the dashboard. */
public record SagaSummaryDto(
        String sagaId,
        String orderId,
        String status,
        String currentStep,
        String customerName,
        String productName,
        Integer quantity,
        BigDecimal totalAmount,
        String failedStep,
        boolean manualRecoveryRequired,
        Instant startedAt,
        Instant completedAt,
        long durationMs
) {

    public static SagaSummaryDto from(SagaExecution saga) {
        return new SagaSummaryDto(
                saga.getSagaId(),
                saga.getOrderId(),
                saga.getStatus().name(),
                saga.getCurrentStep() == null ? null : saga.getCurrentStep().name(),
                saga.getCustomerName(),
                saga.getProductName(),
                saga.getQuantity(),
                saga.getTotalAmount(),
                saga.getFailedStep() == null ? null : saga.getFailedStep().name(),
                saga.isManualRecoveryRequired(),
                saga.getStartedAt(),
                saga.getCompletedAt(),
                saga.getDurationMs()
        );
    }
}
