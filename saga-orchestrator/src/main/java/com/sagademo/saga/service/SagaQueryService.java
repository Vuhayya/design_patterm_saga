package com.sagademo.saga.service;

import com.sagademo.saga.dto.SagaDetailResponse;
import com.sagademo.saga.dto.SagaSummaryDto;
import java.util.List;

/**
 * READ side of the orchestrator: assembles the payload of {@code GET /api/sagas/{sagaId}}.
 *
 * <p>Contract only. Controllers and other services depend on this interface, never on the
 * implementation class.
 *
 * @see com.sagademo.saga.service.impl.SagaQueryServiceImpl
 */
public interface SagaQueryService {

    SagaDetailResponse getSagaDetail(String sagaId, boolean includeLiveState);

    List<SagaSummaryDto> listSagas(int limit);
}
