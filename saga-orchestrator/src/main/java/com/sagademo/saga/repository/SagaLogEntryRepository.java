package com.sagademo.saga.repository;

import com.sagademo.saga.entity.SagaLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** The human-readable execution log of every saga. */
public interface SagaLogEntryRepository extends JpaRepository<SagaLogEntry, Long> {

    List<SagaLogEntry> findAllBySagaIdOrderBySequenceNoAsc(String sagaId);

    long countBySagaId(String sagaId);
}
