package com.sagademo.saga.dto;

import com.sagademo.saga.entity.SagaStepExecution;

import java.time.Instant;

/** One box in the frontend Saga timeline. */
public record SagaStepDto(
        String step,
        String type,
        String status,
        String targetService,
        String compensationFor,
        int sequence,
        Instant startedAt,
        Instant completedAt,
        long durationMs,
        int attempts,
        String errorMessage
) {

    public static SagaStepDto from(SagaStepExecution step) {
        return new SagaStepDto(
                step.getStepName().name(),
                step.getStepType().name(),
                step.getStatus().name(),
                step.getTargetService(),
                step.getCompensationFor() == null ? null : step.getCompensationFor().name(),
                step.getSequenceNo(),
                step.getStartedAt(),
                step.getCompletedAt(),
                step.getDurationMs(),
                step.getAttempts() == null ? 1 : step.getAttempts(),
                step.getErrorMessage()
        );
    }
}
