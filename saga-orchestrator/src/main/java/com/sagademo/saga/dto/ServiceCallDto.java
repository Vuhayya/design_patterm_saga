package com.sagademo.saga.dto;

import com.sagademo.saga.entity.ServiceCallLog;

import java.time.Instant;

/** One row in the frontend "SERVICE COMMUNICATION" panel - a real HTTP call that happened. */
public record ServiceCallDto(
        int sequence,
        String step,
        String stepType,
        String sourceService,
        String targetService,
        String method,
        String endpoint,
        Integer responseStatus,
        String outcome,
        String requestSummary,
        String responseSummary,
        Instant timestamp,
        long durationMs
) {

    public static ServiceCallDto from(ServiceCallLog call) {
        return new ServiceCallDto(
                call.getSequenceNo(),
                call.getStepName() == null ? null : call.getStepName().name(),
                call.getStepType() == null ? null : call.getStepType().name(),
                call.getSourceService(),
                call.getTargetService(),
                call.getHttpMethod(),
                call.getEndpoint(),
                call.getResponseStatus(),
                call.getOutcome(),
                call.getRequestSummary(),
                call.getResponseSummary(),
                call.getCalledAt(),
                call.getDurationMs()
        );
    }
}
