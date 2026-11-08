package com.sagademo.saga.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** What payment-service answers for process/refund. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentClientResponse(
        String paymentId,
        String sagaId,
        String orderId,
        BigDecimal amount,
        String status,
        String transactionReference,
        String refundReference
) {
}
