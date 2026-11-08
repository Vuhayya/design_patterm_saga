package com.sagademo.saga.repository;

import com.sagademo.saga.entity.SagaExecution;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** The Saga database: the orchestrator's own persistence, separate from all business data. */
public interface SagaExecutionRepository extends JpaRepository<SagaExecution, String> {

    List<SagaExecution> findAllByOrderBySequenceNoDesc(Limit limit);

    @Query("select coalesce(max(s.sequenceNo), 0) from SagaExecution s")
    long findMaxSequenceNo();
}
