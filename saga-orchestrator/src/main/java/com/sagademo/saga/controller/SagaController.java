package com.sagademo.saga.controller;

import com.sagademo.saga.dto.SagaDetailResponse;
import com.sagademo.saga.dto.SagaSummaryDto;
import com.sagademo.saga.dto.StartSagaRequest;
import com.sagademo.saga.dto.StartSagaResponse;
import com.sagademo.saga.entity.SagaExecution;
import com.sagademo.saga.service.SagaExecutionLauncher;
import com.sagademo.saga.service.SagaOrchestratorService;
import com.sagademo.saga.service.SagaQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * THE SAGA API the frontend uses.
 *
 * <pre>
 *   POST /api/sagas/orders          start a distributed transaction  -> { sagaId, status }
 *   GET  /api/sagas/{sagaId}        the real execution history       -> steps, HTTP calls, logs
 *   GET  /api/sagas                 recent sagas
 * </pre>
 *
 * <p>Thin by design: it starts or reads a saga and nothing else. The algorithm lives in
 * {@link SagaOrchestratorService}.
 */
@RestController
@RequestMapping("/api/sagas")
@RequiredArgsConstructor
public class SagaController {

    private final SagaOrchestratorService orchestrator;
    private final SagaExecutionLauncher launcher;
    private final SagaQueryService queryService;

    /**
     * STARTS THE DISTRIBUTED TRANSACTION.
     *
     * @param async {@code true} (default) returns immediately with status RUNNING and lets the
     *              browser poll, which is what makes the UI animate step by step.
     *              {@code false} blocks until the saga reached its end state - handy for curl
     *              and for scripted end-to-end tests.
     */
    @PostMapping("/orders")
    public ResponseEntity<StartSagaResponse> startSaga(@Valid @RequestBody StartSagaRequest request,
                                                      @RequestParam(defaultValue = "true") boolean async) {
        SagaExecution saga = orchestrator.prepare(request);
        String sagaId = saga.getSagaId();

        if (async) {
            launcher.launch(sagaId, request);
            return ResponseEntity.accepted().body(new StartSagaResponse(
                    true, sagaId, null, "RUNNING", "/api/sagas/" + sagaId,
                    "Saga started - poll the pollUrl to follow the real execution"));
        }

        orchestrator.execute(sagaId, request);
        SagaDetailResponse detail = queryService.getSagaDetail(sagaId, false);
        return ResponseEntity.status(HttpStatus.CREATED).body(new StartSagaResponse(
                true, sagaId, detail.orderId(), detail.status(), "/api/sagas/" + sagaId,
                detail.failureReason() == null ? "Saga finished" : detail.failureReason()));
    }

    /** THE REAL EXECUTION HISTORY of one distributed transaction (requirement #7). */
    @GetMapping("/{sagaId}")
    public ResponseEntity<SagaDetailResponse> getSaga(
            @PathVariable String sagaId,
            @RequestParam(defaultValue = "true") boolean includeLiveState) {
        return ResponseEntity.ok(queryService.getSagaDetail(sagaId, includeLiveState));
    }

    /** Recent sagas, newest first - the history list on the dashboard. */
    @GetMapping
    public ResponseEntity<List<SagaSummaryDto>> listSagas(@RequestParam(defaultValue = "25") int limit) {
        return ResponseEntity.ok(queryService.listSagas(Math.clamp(limit, 1, 200)));
    }
}
