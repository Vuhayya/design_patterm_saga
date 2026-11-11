package com.sagademo.saga.service;

import com.sagademo.saga.dto.StartSagaRequest;

/**
 * Runs a prepared saga on the background thread pool so the HTTP request that started it can return the {@code sagaId} straight away.
 *
 * <p>Contract only. Controllers and other services depend on this interface, never on the
 * implementation class.
 *
 * @see com.sagademo.saga.service.impl.SagaExecutionLauncherImpl
 */
public interface SagaExecutionLauncher {

    void launch(String sagaId, StartSagaRequest request);
}
