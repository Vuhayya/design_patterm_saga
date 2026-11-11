package com.sagademo.saga.service;

import com.sagademo.saga.client.RemoteServiceException;
import com.sagademo.saga.config.SagaProperties;
import com.sagademo.saga.dto.FailureSimulationDto;
import com.sagademo.saga.dto.StartSagaRequest;
import com.sagademo.saga.dto.SystemStateDto;
import com.sagademo.saga.entity.SagaExecution;
import com.sagademo.saga.event.SagaEvent;
import com.sagademo.saga.event.SagaEventPublisher;
import com.sagademo.saga.saga.SagaContext;
import com.sagademo.saga.saga.SagaDefinition;
import com.sagademo.saga.saga.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * ======================================================================================
 *                      T H E   S A G A   O R C H E S T R A T O R
 * ======================================================================================
 *
 * <p>This class is the answer to "what is Saga Orchestration?". It is a plain, readable state
 * machine - no framework magic, no annotations doing the work behind your back.
 *
 * <p><b>Forward path</b>
 * <pre>
 *   START SAGA
 *       |
 *       v
 *   for each step in [CREATE_ORDER, RESERVE_INVENTORY, PROCESS_PAYMENT, CONFIRM_ORDER]:
 *       call the owning microservice over HTTP   (one LOCAL TRANSACTION, committed there)
 *       push the step on the "completed" stack
 *       |
 *       v
 *   SAGA COMPLETED
 * </pre>
 *
 * <p><b>Failure path</b>
 * <pre>
 *   STEP FAILED
 *       |
 *       v
 *   pop the completed steps one by one  (= reverse order)
 *       |
 *       v
 *   call each step's COMPENSATING transaction
 *       |
 *       v
 *   SAGA COMPENSATED            (or COMPENSATION_FAILED if an undo itself fails)
 * </pre>
 *
 * <p><b>Why a stack?</b> Because "which operations must be undone?" is exactly "which operations
 * have already succeeded?", and LIFO order is the only safe order to unwind them in. The stack IS
 * the answer to the lecturer's question "how does the system know what to compensate?".
 *
 * <p><b>Note what is NOT here:</b> there is no {@code @Transactional} on this class, and there
 * cannot be. A transaction is a property of ONE database connection; this method touches four
 * services and four databases. That absence is the whole reason the Saga pattern exists.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaOrchestratorService {

    private static final String PHASE_EXECUTION = "EXECUTION";
    private static final String PHASE_COMPENSATION = "COMPENSATION";

    private final SagaDefinition sagaDefinition;
    private final SagaExecutionRecorder recorder;
    private final SystemStateService systemStateService;
    private final SagaEventPublisher eventPublisher;
    private final SagaProperties properties;

    // ====================================================================================
    // 1. Start
    // ====================================================================================

    /**
     * Creates the saga row (state PENDING) and captures the "BEFORE" snapshot of the system.
     * Does NOT execute anything yet - the caller decides whether to run it in the background
     * (so the UI can animate) or synchronously (tests).
     */
    public SagaExecution prepare(StartSagaRequest request) {
        List<SagaStep> steps = sagaDefinition.steps();
        SystemStateDto stateBefore = systemStateService.capture(
                "BEFORE SAGA", request.getProductId(), null, null);

        SagaExecution saga = recorder.createSaga(request, steps, stateBefore);

        recorder.log(saga.getSagaId(), "INFO", PHASE_EXECUTION,
                "Đã tạo saga cho khách %s | %s x%d | tổng %s"
                        .formatted(request.getCustomerName(), request.getProductId(),
                                request.getQuantity(), saga.getTotalAmount()));
        describePlannedFailures(saga.getSagaId(), request.getFailureSimulation());

        eventPublisher.publish(SagaEvent.of("SAGA_CREATED", saga.getSagaId(), null, null,
                "PENDING", "Distributed transaction registered"));
        return saga;
    }

    // ====================================================================================
    // 2. Execute the forward path
    // ====================================================================================

    /**
     * Runs the whole distributed transaction and, if anything fails, its compensation.
     *
     * <p>This method never throws: a Saga that fails is a normal, expected outcome that must be
     * recorded, not an error to propagate. The caller learns what happened by reading the saga.
     */
    public void execute(String sagaId, StartSagaRequest request) {
        MDC.put("sagaId", sagaId);
        try {
            SagaContext context = buildContext(sagaId, request);
            Deque<SagaStep> completedSteps = new ArrayDeque<>();

            recorder.markRunning(sagaId);
            recorder.log(sagaId, "INFO", PHASE_EXECUTION, "Saga bắt đầu chạy");
            eventPublisher.publish(SagaEvent.of("SAGA_STARTED", sagaId, null, null, "RUNNING", null));

            for (SagaStep step : sagaDefinition.steps()) {
                boolean ok = runForwardStep(step, context, completedSteps);
                if (!ok) {
                    // A local transaction was refused. Everything that already committed in the
                    // OTHER services must now be undone by compensating transactions.
                    compensate(completedSteps, context);
                    return;
                }
            }

            finishSuccessfully(context);

        } catch (RuntimeException unexpected) {
            // Defensive: a bug in the orchestrator itself must still leave a readable saga.
            log.error("Unexpected orchestrator error for saga {}", sagaId, unexpected);
            recorder.log(sagaId, "ERROR", PHASE_EXECUTION,
                    "Lỗi không mong muốn trong orchestrator: " + unexpected.getMessage());
            recorder.markCompensationFailed(sagaId,
                    "Chính orchestrator bị lỗi: " + unexpected.getMessage()
                            + ". Hãy kiểm tra thủ công database của ba service.");
        } finally {
            MDC.clear();
        }
    }

    /**
     * Executes ONE forward step.
     *
     * @return true when the local transaction committed, false when the service refused it
     */
    private boolean runForwardStep(SagaStep step, SagaContext context, Deque<SagaStep> completedSteps) {
        String sagaId = context.getSagaId();

        pauseForPresentation();
        recorder.updateCurrentStep(sagaId, step.name());
        recorder.forwardStepStarted(sagaId, step);
        recorder.log(sagaId, "INFO", PHASE_EXECUTION,
                "%s -> đang gọi %s ...".formatted(step.name(), step.targetService()));
        eventPublisher.publish(SagaEvent.of("STEP_STARTED", sagaId, context.getOrderId(),
                step.name(), "RUNNING", "calling " + step.targetService()));

        try {
            step.execute(context);

            // Persist the ids the step produced, so a compensation still works after a restart.
            recorder.updateCorrelationIds(sagaId, context.getOrderId(),
                    context.getReservationId(), context.getPaymentId());
            recorder.forwardStepCompleted(sagaId, step);

            // LIFO: the last step that succeeded is the first one to be undone.
            completedSteps.push(step);

            recorder.log(sagaId, "SUCCESS", PHASE_EXECUTION,
                    "%s hoàn thành%s".formatted(step.name(), describeResult(step, context)));
            eventPublisher.publish(SagaEvent.of("STEP_COMPLETED", sagaId, context.getOrderId(),
                    step.name(), "COMPLETED", null));
            return true;

        } catch (RemoteServiceException failure) {
            recorder.forwardStepFailed(sagaId, step, failure.describe());
            recorder.markFailed(sagaId, step.name(), failure.describe());
            recorder.markRemainingStepsSkipped(sagaId);
            recorder.log(sagaId, "ERROR", PHASE_EXECUTION,
                    "%s LỖI tại %s: %s".formatted(step.name(), failure.getService(), failure.getMessage()));
            eventPublisher.publish(SagaEvent.of("STEP_FAILED", sagaId, context.getOrderId(),
                    step.name(), "FAILED", failure.getMessage()));

            context.setCompensationReason("Saga %s được bù trừ vì bước %s lỗi tại %s: %s"
                    .formatted(sagaId, step.name(), failure.getService(), failure.getMessage()));
            return false;
        }
    }

    private void finishSuccessfully(SagaContext context) {
        String sagaId = context.getSagaId();
        recorder.markCompleted(sagaId);
        recorder.log(sagaId, "SUCCESS", PHASE_EXECUTION,
                "SAGA COMPLETED - cả 4 local transaction đã commit, đơn %s đã CONFIRMED"
                        .formatted(context.getOrderId()));
        recorder.saveStateAfter(sagaId, systemStateService.capture(
                "AFTER SAGA (COMPLETED)", context.getProductId(),
                context.getOrderId(), context.getPaymentId()));
        eventPublisher.publish(SagaEvent.of("SAGA_COMPLETED", sagaId, context.getOrderId(),
                null, "COMPLETED", "all steps committed"));
        log.info("SAGA {} COMPLETED", sagaId);
    }

    // ====================================================================================
    // 3. Compensate (the reason this project exists)
    // ====================================================================================

    /**
     * Runs the compensating transactions of every step that had already succeeded, in REVERSE
     * order, and records the honest outcome.
     *
     * <p>Two details that are worth pointing out during the presentation:
     * <ul>
     *   <li><b>Best effort, not all-or-nothing.</b> If one compensation fails, the orchestrator
     *       still tries the remaining ones - leaving three things broken instead of one would be
     *       worse. The saga then ends in COMPENSATION_FAILED.</li>
     *   <li><b>Retries.</b> Each compensation is retried up to
     *       {@code saga.compensation-retry-attempts} times. That is safe only because every
     *       compensating endpoint in this project is idempotent.</li>
     * </ul>
     */
    private void compensate(Deque<SagaStep> completedSteps, SagaContext context) {
        String sagaId = context.getSagaId();

        recorder.markCompensating(sagaId);
        recorder.log(sagaId, "WARN", PHASE_COMPENSATION,
                "BẮT ĐẦU COMPENSATION - %d bước đã commit cần hoàn tác theo thứ tự ngược: %s"
                        .formatted(completedSteps.size(), namesOf(completedSteps)));
        eventPublisher.publish(SagaEvent.of("COMPENSATION_STARTED", sagaId, context.getOrderId(),
                null, "COMPENSATING", completedSteps.size() + " step(s) to undo"));

        List<String> failures = new ArrayList<>();

        while (!completedSteps.isEmpty()) {
            SagaStep step = completedSteps.pop();

            if (!step.hasCompensation()) {
                recorder.log(sagaId, "INFO", PHASE_COMPENSATION,
                        "%s không cần compensating transaction (bản thân nó chưa commit gì)"
                                .formatted(step.name()));
                continue;
            }

            compensateOneStep(step, context, failures);
        }

        if (failures.isEmpty()) {
            recorder.markCompensated(sagaId);
            recorder.log(sagaId, "SUCCESS", PHASE_COMPENSATION,
                    "SAGA COMPENSATED - mọi bước đã commit đều được hoàn tác, hệ thống nhất quán trở lại");
            eventPublisher.publish(SagaEvent.of("SAGA_COMPENSATED", sagaId, context.getOrderId(),
                    null, "COMPENSATED", null));
            log.warn("SAGA {} COMPENSATED", sagaId);
        } else {
            String hint = "Compensation thất bại ở: %s. Hệ thống đang BẤT NHẤT và cần xử lý "
                    .formatted(String.join(", ", failures))
                    + "thủ công (chạy lại compensation, hoặc sửa dữ liệu ở service sở hữu nó). "
                    + "Đây là giới hạn thật của Saga pattern: nó không đảm bảo hoàn tác được.";
            recorder.markCompensationFailed(sagaId, hint);
            recorder.log(sagaId, "ERROR", PHASE_COMPENSATION, "SAGA COMPENSATION FAILED - " + hint);
            eventPublisher.publish(SagaEvent.of("SAGA_COMPENSATION_FAILED", sagaId, context.getOrderId(),
                    null, "COMPENSATION_FAILED", hint));
            log.error("SAGA {} COMPENSATION FAILED | {}", sagaId, hint);
        }

        recorder.saveStateAfter(sagaId, systemStateService.capture(
                failures.isEmpty() ? "AFTER COMPENSATION" : "AFTER FAILED COMPENSATION",
                context.getProductId(), context.getOrderId(), context.getPaymentId()));
    }

    /** Compensates one step, retrying up to the configured number of attempts. */
    private void compensateOneStep(SagaStep step, SagaContext context, List<String> failures) {
        String sagaId = context.getSagaId();
        int maxAttempts = Math.max(1, properties.getCompensationRetryAttempts());

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            pauseForPresentation();
            recorder.compensationStarted(sagaId, step);
            recorder.log(sagaId, "WARN", PHASE_COMPENSATION,
                    "%s -> hoàn tác %s qua %s%s".formatted(
                            step.compensationName(), step.name(),
                            step.compensationAction().targetService(),
                            attempt > 1 ? " (thử lại %d/%d)".formatted(attempt, maxAttempts) : ""));
            eventPublisher.publish(SagaEvent.of("COMPENSATION_STEP_STARTED", sagaId, context.getOrderId(),
                    step.compensationName(), "RUNNING", "attempt " + attempt));

            try {
                step.compensate(context);
                recorder.compensationCompleted(sagaId, step);
                recorder.log(sagaId, "SUCCESS", PHASE_COMPENSATION,
                        "%s hoàn thành - %s đã được hoàn tác".formatted(step.compensationName(), step.name()));
                eventPublisher.publish(SagaEvent.of("COMPENSATION_STEP_COMPLETED", sagaId,
                        context.getOrderId(), step.compensationName(), "COMPENSATED", null));
                return;

            } catch (RemoteServiceException failure) {
                recorder.compensationFailed(sagaId, step, failure.describe());
                recorder.log(sagaId, "ERROR", PHASE_COMPENSATION,
                        "%s LỖI (lần %d/%d): %s".formatted(
                                step.compensationName(), attempt, maxAttempts, failure.getMessage()));
                eventPublisher.publish(SagaEvent.of("COMPENSATION_STEP_FAILED", sagaId,
                        context.getOrderId(), step.compensationName(), "COMPENSATION_FAILED",
                        failure.getMessage()));

                if (attempt == maxAttempts) {
                    failures.add("%s (%s)".formatted(step.compensationName(), failure.getErrorCode()));
                }
            }
        }
    }

    // ====================================================================================
    // helpers
    // ====================================================================================

    private SagaContext buildContext(String sagaId, StartSagaRequest request) {
        BigDecimal totalAmount = request.getUnitPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));
        return SagaContext.builder()
                .sagaId(sagaId)
                .customerName(request.getCustomerName())
                .productId(request.getProductId())
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalAmount(totalAmount)
                .failures(request.getFailureSimulation() == null
                        ? new FailureSimulationDto() : request.getFailureSimulation())
                .build();
    }

    /** Extra detail in the live log, so the audience sees the ids flowing between services. */
    private String describeResult(SagaStep step, SagaContext context) {
        return switch (step.name()) {
            case CREATE_ORDER -> " (orderId=" + context.getOrderId() + ")";
            case RESERVE_INVENTORY -> " (reservationId=" + context.getReservationId() + ")";
            case PROCESS_PAYMENT -> " (paymentId=" + context.getPaymentId() + ")";
            default -> "";
        };
    }

    private void describePlannedFailures(String sagaId, FailureSimulationDto failures) {
        if (failures == null) {
            return;
        }
        if (failures.anyForwardFailure()) {
            recorder.log(sagaId, "WARN", PHASE_EXECUTION,
                    "Đang BẬT giả lập lỗi cho bước forward: " + describeFlags(failures, true));
        }
        if (failures.anyCompensationFailure()) {
            recorder.log(sagaId, "WARN", PHASE_EXECUTION,
                    "Đang BẬT giả lập lỗi cho compensating transaction: "
                            + describeFlags(failures, false));
        }
    }

    private String describeFlags(FailureSimulationDto f, boolean forward) {
        List<String> on = new ArrayList<>();
        if (forward) {
            if (f.isFailCreateOrder()) {
                on.add("CREATE_ORDER");
            }
            if (f.isFailReserveInventory()) {
                on.add("RESERVE_INVENTORY");
            }
            if (f.isFailProcessPayment()) {
                on.add("PROCESS_PAYMENT");
            }
            if (f.isFailConfirmOrder()) {
                on.add("CONFIRM_ORDER");
            }
        } else {
            if (f.isFailReleaseInventory()) {
                on.add("RELEASE_INVENTORY");
            }
            if (f.isFailRefundPayment()) {
                on.add("REFUND_PAYMENT");
            }
            if (f.isFailCancelOrder()) {
                on.add("CANCEL_ORDER");
            }
        }
        return String.join(", ", on);
    }

    private String namesOf(Deque<SagaStep> steps) {
        return steps.stream().map(step -> step.name().name()).reduce((a, b) -> a + " <- " + b).orElse("none");
    }

    /**
     * A deliberate pause before each step. It has nothing to do with the Saga pattern - it exists
     * so that a projector audience can watch the timeline fill in box by box instead of seeing
     * the whole thing flash green in 200 ms. Set {@code saga.step-delay-ms: 0} to switch it off.
     */
    private void pauseForPresentation() {
        long delay = properties.getStepDelayMs();
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
