package com.sagademo.saga.repository;

import com.sagademo.saga.entity.SagaStepExecution;
import com.sagademo.saga.entity.SagaStepName;
import com.sagademo.saga.entity.StepType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Step-by-step history of every saga. */
public interface SagaStepExecutionRepository extends JpaRepository<SagaStepExecution, Long> {

    List<SagaStepExecution> findAllBySagaIdOrderBySequenceNoAsc(String sagaId);

    Optional<SagaStepExecution> findBySagaIdAndStepNameAndStepType(
            String sagaId, SagaStepName stepName, StepType stepType);

    void deleteAllBySagaId(String sagaId);
}
