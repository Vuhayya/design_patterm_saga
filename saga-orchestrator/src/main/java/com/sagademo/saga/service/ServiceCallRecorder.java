package com.sagademo.saga.service;

import com.sagademo.saga.client.SagaCallContext;
import java.time.Instant;

/**
 * Writes one {@code ServiceCallLog} row per real HTTP call between services.
 *
 * <p>Contract only. Controllers and other services depend on this interface, never on the
 * implementation class.
 *
 * @see com.sagademo.saga.service.impl.ServiceCallRecorderImpl
 */
public interface ServiceCallRecorder {

    void record(SagaCallContext context,
                String targetService,
                String httpMethod,
                String endpoint,
                Integer responseStatus,
                boolean success,
                String requestSummary,
                String responseSummary,
                Instant calledAt,
                long durationMs);
}
