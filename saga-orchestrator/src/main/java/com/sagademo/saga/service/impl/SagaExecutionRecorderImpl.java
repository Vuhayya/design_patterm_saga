package com.sagademo.saga.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sagademo.saga.dto.StartSagaRequest;
import com.sagademo.saga.dto.SystemStateDto;
import com.sagademo.saga.entity.SagaExecution;
import com.sagademo.saga.entity.SagaLogEntry;
import com.sagademo.saga.entity.SagaStatus;
import com.sagademo.saga.entity.SagaStepExecution;
import com.sagademo.saga.entity.SagaStepName;
import com.sagademo.saga.entity.StepStatus;
import com.sagademo.saga.entity.StepType;
import com.sagademo.saga.exception.SagaNotFoundException;
import com.sagademo.saga.repository.SagaExecutionRepository;
import com.sagademo.saga.repository.SagaLogEntryRepository;
import com.sagademo.saga.repository.SagaStepExecutionRepository;
import com.sagademo.saga.saga.SagaStep;
import com.sagademo.saga.service.SagaExecutionRecorder;
import com.sagademo.saga.service.SagaOrchestratorService;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * ALL WRITES to the Saga database live here, so that {@code SagaOrchestratorService} contains
 * nothing but the Saga algorithm itself.
 *
 * <p>Two deliberate design decisions worth explaining during the presentation:
 *
 * <ol>
 *   <li><b>Every method is its own short transaction</b> ({@code REQUIRES_NEW}). The Saga as a
 *       whole is NOT one transaction - it cannot be, it spans four services. What IS transactional
 *       is each little bookkeeping write. This is why the state you see in the UI is durable at
 *       every moment, even if the orchestrator is killed halfway through.</li>
 *   <li><b>The state is written BEFORE and AFTER each remote call.</b> If the process died between
 *       the two, the row would stay in {@code RUNNING} - which is precisely how a real
 *       orchestrator detects "stuck" sagas after a crash and resumes or compensates them.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaExecutionRecorderImpl implements SagaExecutionRecorder {

    private final SagaExecutionRepository sagaRepository;
    private final SagaStepExecutionRepository stepRepository;
    private final SagaLogEntryRepository logRepository;
    private final ObjectMapper objectMapper;

    private final AtomicLong sagaSequence = new AtomicLong(0);

    @PostConstruct
    void initSequence() {
        sagaSequence.set(sagaRepository.findMaxSequenceNo());
    }

    // ====================================================================================
    // Saga lifecycle
    // ====================================================================================

    /**
     * Creates the saga row in state PENDING and pre-creates the four forward steps in state
     * PENDING, so the frontend can draw the whole plan before anything has happened yet.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SagaExecution createSaga(StartSagaRequest request, List<SagaStep> steps, SystemStateDto stateBefore) {
        long next = sagaSequence.incrementAndGet();
        String sagaId = "SAGA-%s-%04d".formatted(LocalDate.now().toString().replace("-", ""), next);

        BigDecimal totalAmount = request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        SagaExecution saga = SagaExecution.builder()
                .sagaId(sagaId)
                .status(SagaStatus.PENDING)
                .customerName(request.getCustomerName())
                .productId(request.getProductId())
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalAmount(totalAmount)
                .compensationRequired(false)
                .manualRecoveryRequired(false)
                .startedAt(Instant.now())
                .stateBeforeJson(toJson(stateBefore))
                .sequenceNo(next)
                .build();
        sagaRepository.save(saga);

        int sequence = 1;
        for (SagaStep step : steps) {
            stepRepository.save(SagaStepExecution.builder()
                    .sagaId(sagaId)
                    .stepName(step.name())
                    .stepType(StepType.FORWARD)
                    .status(StepStatus.PENDING)
                    .targetService(step.targetService())
                    .sequenceNo(sequence++)
                    .attempts(0)
                    .build());
        }
        return saga;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRunning(String sagaId) {
        SagaExecution saga = find(sagaId);
        saga.setStatus(SagaStatus.RUNNING);
        sagaRepository.save(saga);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String sagaId) {
        SagaExecution saga = find(sagaId);
        saga.setStatus(SagaStatus.COMPLETED);
        saga.setCompletedAt(Instant.now());
        sagaRepository.save(saga);
    }

    /** A forward step failed. Transient state: compensation starts right after this. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String sagaId, SagaStepName failedStep, String reason) {
        SagaExecution saga = find(sagaId);
        saga.setStatus(SagaStatus.FAILED);
        saga.setFailedStep(failedStep);
        saga.setFailureReason(truncate(reason, 1000));
        saga.setCurrentStep(failedStep);
        sagaRepository.save(saga);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompensating(String sagaId) {
        SagaExecution saga = find(sagaId);
        saga.setStatus(SagaStatus.COMPENSATING);
        saga.setCompensationRequired(true);
        sagaRepository.save(saga);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompensated(String sagaId) {
        SagaExecution saga = find(sagaId);
        saga.setStatus(SagaStatus.COMPENSATED);
        saga.setCompletedAt(Instant.now());
        sagaRepository.save(saga);
    }

    /**
     * The honest end state: at least one compensating transaction failed, so the system is
     * inconsistent and a human has to fix it (requirement #28).
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompensationFailed(String sagaId, String recoveryHint) {
        SagaExecution saga = find(sagaId);
        saga.setStatus(SagaStatus.COMPENSATION_FAILED);
        saga.setManualRecoveryRequired(true);
        saga.setRecoveryHint(truncate(recoveryHint, 1000));
        saga.setCompletedAt(Instant.now());
        sagaRepository.save(saga);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCurrentStep(String sagaId, SagaStepName step) {
        SagaExecution saga = find(sagaId);
        saga.setCurrentStep(step);
        sagaRepository.save(saga);
    }

    /** Stores the ids of the resources the saga created in the other services. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCorrelationIds(String sagaId, String orderId, String reservationId, String paymentId) {
        SagaExecution saga = find(sagaId);
        if (orderId != null) {
            saga.setOrderId(orderId);
        }
        if (reservationId != null) {
            saga.setReservationId(reservationId);
        }
        if (paymentId != null) {
            saga.setPaymentId(paymentId);
        }
        sagaRepository.save(saga);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveStateAfter(String sagaId, SystemStateDto stateAfter) {
        SagaExecution saga = find(sagaId);
        saga.setStateAfterJson(toJson(stateAfter));
        sagaRepository.save(saga);
    }

    // ====================================================================================
    // Step lifecycle - forward
    // ====================================================================================

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void forwardStepStarted(String sagaId, SagaStep step) {
        SagaStepExecution row = findStep(sagaId, step.name(), StepType.FORWARD);
        row.setStatus(StepStatus.RUNNING);
        row.setStartedAt(Instant.now());
        row.setAttempts(row.getAttempts() == null ? 1 : row.getAttempts() + 1);
        stepRepository.save(row);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void forwardStepCompleted(String sagaId, SagaStep step) {
        SagaStepExecution row = findStep(sagaId, step.name(), StepType.FORWARD);
        row.setStatus(StepStatus.COMPLETED);
        row.setCompletedAt(Instant.now());
        stepRepository.save(row);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void forwardStepFailed(String sagaId, SagaStep step, String errorMessage) {
        SagaStepExecution row = findStep(sagaId, step.name(), StepType.FORWARD);
        row.setStatus(StepStatus.FAILED);
        row.setCompletedAt(Instant.now());
        row.setErrorMessage(truncate(errorMessage, 1000));
        stepRepository.save(row);
    }

    /** Marks the forward steps that were never reached, so the timeline shows them greyed out. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRemainingStepsSkipped(String sagaId) {
        stepRepository.findAllBySagaIdOrderBySequenceNoAsc(sagaId).stream()
                .filter(row -> row.getStepType() == StepType.FORWARD)
                .filter(row -> row.getStatus() == StepStatus.PENDING)
                .forEach(row -> {
                    row.setStatus(StepStatus.SKIPPED);
                    stepRepository.save(row);
                });
    }

    // ====================================================================================
    // Step lifecycle - compensation
    // ====================================================================================

    /**
     * Creates the COMPENSATION row for a completed forward step. It is created only when
     * compensation actually starts - so a successful saga has no compensation rows at all.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensationStarted(String sagaId, SagaStep step) {
        int sequence = stepRepository.findAllBySagaIdOrderBySequenceNoAsc(sagaId).size() + 1;
        SagaStepExecution row = stepRepository
                .findBySagaIdAndStepNameAndStepType(sagaId, step.compensationName(), StepType.COMPENSATION)
                .orElseGet(() -> SagaStepExecution.builder()
                        .sagaId(sagaId)
                        .stepName(step.compensationName())
                        .stepType(StepType.COMPENSATION)
                        .targetService(step.compensationAction().targetService())
                        .compensationFor(step.name())
                        .sequenceNo(sequence)
                        .attempts(0)
                        .status(StepStatus.PENDING)
                        .build());
        row.setStatus(StepStatus.RUNNING);
        row.setStartedAt(row.getStartedAt() == null ? Instant.now() : row.getStartedAt());
        row.setAttempts(row.getAttempts() == null ? 1 : row.getAttempts() + 1);
        stepRepository.save(row);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensationCompleted(String sagaId, SagaStep step) {
        SagaStepExecution row = findStep(sagaId, step.compensationName(), StepType.COMPENSATION);
        row.setStatus(StepStatus.COMPENSATED);
        row.setCompletedAt(Instant.now());
        row.setErrorMessage(null);
        stepRepository.save(row);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensationFailed(String sagaId, SagaStep step, String errorMessage) {
        SagaStepExecution row = findStep(sagaId, step.compensationName(), StepType.COMPENSATION);
        row.setStatus(StepStatus.COMPENSATION_FAILED);
        row.setCompletedAt(Instant.now());
        row.setErrorMessage(truncate(errorMessage, 1000));
        stepRepository.save(row);
    }

    // ====================================================================================
    // Human readable log (the LIVE LOG panel)
    // ====================================================================================

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String sagaId, String level, String phase, String message) {
        int sequence = (int) logRepository.countBySagaId(sagaId) + 1;
        logRepository.save(SagaLogEntry.builder()
                .sagaId(sagaId)
                .sequenceNo(sequence)
                .level(level)
                .phase(phase)
                .message(truncate(message, 1000))
                .loggedAt(Instant.now())
                .build());
    }

    // ====================================================================================
    // helpers
    // ====================================================================================

    private SagaExecution find(String sagaId) {
        return sagaRepository.findById(sagaId).orElseThrow(() -> new SagaNotFoundException(sagaId));
    }

    private SagaStepExecution findStep(String sagaId, SagaStepName stepName, StepType type) {
        return stepRepository.findBySagaIdAndStepNameAndStepType(sagaId, stepName, type)
                .orElseThrow(() -> new IllegalStateException(
                        "No %s step row %s for saga %s".formatted(type, stepName, sagaId)));
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("Could not serialise state snapshot: {}", ex.getMessage());
            return null;
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }
}
