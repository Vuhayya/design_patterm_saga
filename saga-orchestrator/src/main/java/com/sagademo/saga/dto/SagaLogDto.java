package com.sagademo.saga.dto;

import com.sagademo.saga.entity.SagaLogEntry;

import java.time.Instant;

/** One line in the frontend "LIVE LOG" terminal panel. */
public record SagaLogDto(
        int sequence,
        String level,
        String phase,
        String message,
        Instant timestamp
) {

    public static SagaLogDto from(SagaLogEntry entry) {
        return new SagaLogDto(
                entry.getSequenceNo(),
                entry.getLevel(),
                entry.getPhase(),
                entry.getMessage(),
                entry.getLoggedAt()
        );
    }
}
