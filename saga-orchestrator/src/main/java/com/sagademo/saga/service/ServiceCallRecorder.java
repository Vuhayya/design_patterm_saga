package com.sagademo.saga.service;

import com.sagademo.saga.client.SagaCallContext;
import com.sagademo.saga.entity.ServiceCallLog;
import com.sagademo.saga.repository.ServiceCallLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Writes one {@link ServiceCallLog} row per real HTTP call between services.
 *
 * <p>{@code Propagation.REQUIRES_NEW} is important: the audit trail must survive even if the
 * caller later throws. The communication log is evidence of what happened, so it must never be
 * rolled back together with a failing step.
 */
@Service
@RequiredArgsConstructor
public class ServiceCallRecorder {

    private static final String SOURCE_SERVICE = "saga-orchestrator";
    private static final int MAX_SUMMARY = 900;

    private final ServiceCallLogRepository serviceCallLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(SagaCallContext context,
                       String targetService,
                       String httpMethod,
                       String endpoint,
                       Integer responseStatus,
                       boolean success,
                       String requestSummary,
                       String responseSummary,
                       Instant calledAt,
                       long durationMs) {

        int sequence = (int) serviceCallLogRepository.countBySagaId(context.sagaId()) + 1;

        serviceCallLogRepository.save(ServiceCallLog.builder()
                .sagaId(context.sagaId())
                .sequenceNo(sequence)
                .stepName(context.stepName())
                .stepType(context.stepType())
                .sourceService(SOURCE_SERVICE)
                .targetService(targetService)
                .httpMethod(httpMethod)
                .endpoint(endpoint)
                .responseStatus(responseStatus)
                .outcome(success ? "SUCCESS" : "ERROR")
                .requestSummary(truncate(requestSummary))
                .responseSummary(truncate(responseSummary))
                .calledAt(calledAt)
                .durationMs(durationMs)
                .build());
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_SUMMARY ? value : value.substring(0, MAX_SUMMARY) + "...";
    }
}
