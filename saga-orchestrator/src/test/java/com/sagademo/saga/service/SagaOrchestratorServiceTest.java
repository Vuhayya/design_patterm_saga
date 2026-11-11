package com.sagademo.saga.service;

import com.sagademo.saga.service.impl.SagaOrchestratorServiceImpl;
import com.sagademo.saga.client.RemoteServiceException;
import com.sagademo.saga.config.SagaProperties;
import com.sagademo.saga.dto.StartSagaRequest;
import com.sagademo.saga.entity.SagaStepName;
import com.sagademo.saga.event.SagaEventPublisher;
import com.sagademo.saga.saga.SagaContext;
import com.sagademo.saga.saga.SagaDefinition;
import com.sagademo.saga.saga.SagaStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the orchestration logic in isolation: the four steps are in-memory fakes that record
 * every call, so the assertions can check the exact order of forward and compensating calls.
 */
class SagaOrchestratorServiceTest {

    private static final String SAGA_ID = "SAGA-TEST";

    private final List<String> calls = new ArrayList<>();
    private final Set<SagaStepName> failingForward = new HashSet<>();
    /** compensation step -> number of attempts that must fail before it succeeds. */
    private final Map<SagaStepName, Integer> compensationFailuresLeft = new HashMap<>();

    private SagaExecutionRecorder recorder;
    private SagaOrchestratorService orchestrator;
    private StartSagaRequest request;

    @BeforeEach
    void setUp() {
        recorder = mock(SagaExecutionRecorder.class);
        SystemStateService systemStateService = mock(SystemStateService.class);
        SagaEventPublisher publisher = mock(SagaEventPublisher.class);

        SagaProperties properties = new SagaProperties();
        properties.setStepDelayMs(0);
        properties.setCompensationRetryAttempts(2);

        SagaDefinition definition = mock(SagaDefinition.class);
        when(definition.steps()).thenReturn(List.of(
                new FakeStep(SagaStepName.CREATE_ORDER, "order-service"),
                new FakeStep(SagaStepName.RESERVE_INVENTORY, "inventory-service"),
                new FakeStep(SagaStepName.PROCESS_PAYMENT, "payment-service"),
                new FakeStep(SagaStepName.CONFIRM_ORDER, "order-service")));

        orchestrator = new SagaOrchestratorServiceImpl(
                definition, recorder, systemStateService, publisher, properties);

        request = StartSagaRequest.builder()
                .customerName("Alice").productId("P1").productName("Pizza")
                .quantity(2).unitPrice(new BigDecimal("10.00")).build();
    }

    @Test
    void happyPath_runsAllFourStepsAndCompletes_withoutCompensation() {
        orchestrator.execute(SAGA_ID, request);

        assertThat(calls).containsExactly(
                "CREATE_ORDER", "RESERVE_INVENTORY", "PROCESS_PAYMENT", "CONFIRM_ORDER");
        verify(recorder).markCompleted(SAGA_ID);
        verify(recorder, never()).markCompensating(any());
        verify(recorder, never()).markFailed(any(), any(), any());
    }

    @Test
    void paymentFails_compensatesInventoryThenOrder_inReverseOrder() {
        failingForward.add(SagaStepName.PROCESS_PAYMENT);

        orchestrator.execute(SAGA_ID, request);

        assertThat(calls).containsExactly(
                "CREATE_ORDER", "RESERVE_INVENTORY", "PROCESS_PAYMENT(FAILS)",
                "RELEASE_INVENTORY", "CANCEL_ORDER");
        verify(recorder).markFailed(eq(SAGA_ID), eq(SagaStepName.PROCESS_PAYMENT), any());
        verify(recorder).markCompensated(SAGA_ID);
        verify(recorder, never()).markCompleted(any());
        verify(recorder, never()).markCompensationFailed(any(), any());
    }

    @Test
    void inventoryFails_onlyTheOrderIsCancelled() {
        failingForward.add(SagaStepName.RESERVE_INVENTORY);

        orchestrator.execute(SAGA_ID, request);

        assertThat(calls).containsExactly(
                "CREATE_ORDER", "RESERVE_INVENTORY(FAILS)", "CANCEL_ORDER");
        verify(recorder).markCompensated(SAGA_ID);
    }

    @Test
    void firstStepFails_nothingToCompensate_sagaStillEndsCompensated() {
        failingForward.add(SagaStepName.CREATE_ORDER);

        orchestrator.execute(SAGA_ID, request);

        assertThat(calls).containsExactly("CREATE_ORDER(FAILS)");
        verify(recorder).markCompensated(SAGA_ID);
    }

    @Test
    void confirmOrderFails_undoesAllThreeEarlierSteps_inReverseOrder() {
        failingForward.add(SagaStepName.CONFIRM_ORDER);

        orchestrator.execute(SAGA_ID, request);

        assertThat(calls).containsExactly(
                "CREATE_ORDER", "RESERVE_INVENTORY", "PROCESS_PAYMENT", "CONFIRM_ORDER(FAILS)",
                "REFUND_PAYMENT", "RELEASE_INVENTORY", "CANCEL_ORDER");
        verify(recorder).markCompensated(SAGA_ID);
    }

    @Test
    void compensationFailsOnFirstAttempt_isRetriedAndSucceeds() {
        failingForward.add(SagaStepName.PROCESS_PAYMENT);
        compensationFailuresLeft.put(SagaStepName.RELEASE_INVENTORY, 1);

        orchestrator.execute(SAGA_ID, request);

        assertThat(calls).containsSubsequence(
                "RELEASE_INVENTORY(FAILS)", "RELEASE_INVENTORY", "CANCEL_ORDER");
        verify(recorder).markCompensated(SAGA_ID);
        verify(recorder, never()).markCompensationFailed(any(), any());
    }

    @Test
    void compensationKeepsFailing_sagaIsCompensationFailed_butRemainingUndosStillRun() {
        failingForward.add(SagaStepName.CONFIRM_ORDER);
        compensationFailuresLeft.put(SagaStepName.REFUND_PAYMENT, 99);

        orchestrator.execute(SAGA_ID, request);

        assertThat(calls).containsExactly(
                "CREATE_ORDER", "RESERVE_INVENTORY", "PROCESS_PAYMENT", "CONFIRM_ORDER(FAILS)",
                "REFUND_PAYMENT(FAILS)", "REFUND_PAYMENT(FAILS)",   // 2 attempts, then give up
                "RELEASE_INVENTORY", "CANCEL_ORDER");               // best effort continues
        verify(recorder).markCompensationFailed(eq(SAGA_ID), any());
        verify(recorder, never()).markCompensated(any());
    }

    @Test
    void unexpectedBug_neverEscapes_andLeavesAReadableSaga() {
        SagaDefinition broken = mock(SagaDefinition.class);
        when(broken.steps()).thenThrow(new IllegalStateException("boom"));
        SagaOrchestratorService service = new SagaOrchestratorServiceImpl(
                broken, recorder, mock(SystemStateService.class),
                mock(SagaEventPublisher.class), new SagaProperties());

        service.execute(SAGA_ID, request);

        verify(recorder).markCompensationFailed(eq(SAGA_ID), Mockito.contains("boom"));
    }

    /** Records calls instead of talking HTTP; failure behaviour is driven by the test fields. */
    private class FakeStep implements SagaStep {
        private final SagaStepName name;
        private final String service;

        FakeStep(SagaStepName name, String service) {
            this.name = name;
            this.service = service;
        }

        @Override
        public SagaStepName name() {
            return name;
        }

        @Override
        public String targetService() {
            return service;
        }

        @Override
        public void execute(SagaContext context) {
            if (failingForward.contains(name)) {
                calls.add(name + "(FAILS)");
                throw new RemoteServiceException(service, 500, "SIMULATED", name + " refused");
            }
            calls.add(name.name());
        }

        @Override
        public void compensate(SagaContext context) {
            SagaStepName undo = compensationName();
            int left = compensationFailuresLeft.getOrDefault(undo, 0);
            if (left > 0) {
                compensationFailuresLeft.put(undo, left - 1);
                calls.add(undo + "(FAILS)");
                throw new RemoteServiceException(service, 500, "SIMULATED", undo + " refused");
            }
            calls.add(undo.name());
        }
    }
}
