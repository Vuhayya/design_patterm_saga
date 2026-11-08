package com.sagademo.saga.client.dto;

import java.math.BigDecimal;

/**
 * The orchestrator's view of the order-service contract for {@code POST /api/orders}.
 *
 * <p>Deliberately duplicated instead of shared through a common JAR: in a real microservice
 * system each service owns its own classes, and sharing entities/DTOs is what quietly turns a
 * set of services back into a distributed monolith. What they share is the API contract, not code.
 */
public record CreateOrderPayload(
        String sagaId,
        String customerName,
        String productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        boolean simulateFailure
) {
}
