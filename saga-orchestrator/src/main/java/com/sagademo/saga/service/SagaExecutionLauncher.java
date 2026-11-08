package com.sagademo.saga.service;

import com.sagademo.saga.dto.StartSagaRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs a prepared saga on the background thread pool so the HTTP request that started it can
 * return the {@code sagaId} straight away.
 *
 * <p>It is a separate bean on purpose: {@code @Async} only works through the Spring proxy, so
 * calling an {@code @Async} method from inside {@code SagaOrchestratorService} itself would
 * silently run synchronously - a classic Spring trap worth knowing about.
 */
@Service
@RequiredArgsConstructor
public class SagaExecutionLauncher {

    private final SagaOrchestratorService orchestrator;

    @Async("sagaTaskExecutor")
    public void launch(String sagaId, StartSagaRequest request) {
        orchestrator.execute(sagaId, request);
    }
}
