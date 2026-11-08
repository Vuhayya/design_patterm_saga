package com.sagademo.saga.repository;

import com.sagademo.saga.entity.ServiceCallLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Audit of every HTTP call the orchestrator made between services. */
public interface ServiceCallLogRepository extends JpaRepository<ServiceCallLog, Long> {

    List<ServiceCallLog> findAllBySagaIdOrderBySequenceNoAsc(String sagaId);

    long countBySagaId(String sagaId);
}
