package com.sagademo.saga.service;

import com.sagademo.saga.dto.StartSagaRequest;
import com.sagademo.saga.entity.SagaExecution;

/**
 * ====================================================================================== T H E S A G A O R C H E S T R A T O R ======================================================================================
 *
 * <p>Contract only. Controllers and other services depend on this interface, never on the
 * implementation class.
 *
 * @see com.sagademo.saga.service.impl.SagaOrchestratorServiceImpl
 */
public interface SagaOrchestratorService {

    /** Creates the saga row (state PENDING) and captures the "BEFORE" snapshot of the system. */
    SagaExecution prepare(StartSagaRequest request);

    /** Runs the whole distributed transaction and, if anything fails, its compensation. */
    void execute(String sagaId, StartSagaRequest request);
}
