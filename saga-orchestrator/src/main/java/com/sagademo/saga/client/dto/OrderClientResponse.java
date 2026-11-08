package com.sagademo.saga.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** What order-service answers. Only the fields the orchestrator actually needs are mapped. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderClientResponse(
        String orderId,
        String sagaId,
        String status,
        String customerName
) {
}
