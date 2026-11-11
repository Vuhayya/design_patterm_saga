package com.sagademo.saga.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sagademo.saga.dto.SagaDetailResponse;
import com.sagademo.saga.dto.SagaLogDto;
import com.sagademo.saga.dto.SagaStepDto;
import com.sagademo.saga.dto.SagaSummaryDto;
import com.sagademo.saga.dto.ServiceCallDto;
import com.sagademo.saga.dto.SystemStateDto;
import com.sagademo.saga.entity.SagaExecution;
import com.sagademo.saga.exception.SagaNotFoundException;
import com.sagademo.saga.repository.SagaExecutionRepository;
import com.sagademo.saga.repository.SagaLogEntryRepository;
import com.sagademo.saga.repository.SagaStepExecutionRepository;
import com.sagademo.saga.repository.ServiceCallLogRepository;
import com.sagademo.saga.service.SagaQueryService;
import com.sagademo.saga.service.SystemStateService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * READ side of the orchestrator: assembles the payload of {@code GET /api/sagas/{sagaId}}.
 *
 * <p>Everything it returns was written to the Saga database while the orchestrator was really
 * calling the other microservices. The frontend is a pure renderer of this data - which is what
 * requirement #7 ("do not fake the saga") is about.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaQueryServiceImpl implements SagaQueryService {

    private final SagaExecutionRepository sagaRepository;
    private final SagaStepExecutionRepository stepRepository;
    private final ServiceCallLogRepository serviceCallLogRepository;
    private final SagaLogEntryRepository logRepository;
    private final SystemStateService systemStateService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public SagaDetailResponse getSagaDetail(String sagaId, boolean includeLiveState) {
        SagaExecution saga = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new SagaNotFoundException(sagaId));

        List<SagaStepDto> steps = stepRepository.findAllBySagaIdOrderBySequenceNoAsc(sagaId)
                .stream().map(SagaStepDto::from).toList();
        List<ServiceCallDto> calls = serviceCallLogRepository.findAllBySagaIdOrderBySequenceNoAsc(sagaId)
                .stream().map(ServiceCallDto::from).toList();
        List<SagaLogDto> logs = logRepository.findAllBySagaIdOrderBySequenceNoAsc(sagaId)
                .stream().map(SagaLogDto::from).toList();

        SystemStateDto stateNow = null;
        if (includeLiveState) {
            stateNow = systemStateService.capture("NOW", saga.getProductId(),
                    saga.getOrderId(), saga.getPaymentId());
        }

        return new SagaDetailResponse(
                true,
                saga.getSagaId(),
                saga.getOrderId(),
                saga.getStatus().name(),
                saga.getCurrentStep() == null ? null : saga.getCurrentStep().name(),
                saga.getCustomerName(),
                saga.getProductId(),
                saga.getProductName(),
                saga.getQuantity(),
                saga.getUnitPrice(),
                saga.getTotalAmount(),
                saga.getReservationId(),
                saga.getPaymentId(),
                saga.getFailedStep() == null ? null : saga.getFailedStep().name(),
                saga.getFailureReason(),
                saga.isCompensationRequired(),
                saga.isManualRecoveryRequired(),
                saga.getRecoveryHint(),
                saga.getStartedAt(),
                saga.getCompletedAt(),
                saga.getDurationMs(),
                saga.isFinished(),
                steps,
                calls,
                logs,
                readState(saga.getStateBeforeJson()),
                readState(saga.getStateAfterJson()),
                stateNow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SagaSummaryDto> listSagas(int limit) {
        return sagaRepository.findAllByOrderBySequenceNoDesc(Limit.of(limit))
                .stream().map(SagaSummaryDto::from).toList();
    }

    private SystemStateDto readState(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, SystemStateDto.class);
        } catch (Exception ex) {
            log.warn("Could not read stored state snapshot: {}", ex.getMessage());
            return null;
        }
    }
}
