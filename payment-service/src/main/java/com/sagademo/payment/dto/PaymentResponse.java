package com.sagademo.payment.dto;

import com.sagademo.payment.entity.PaymentEntity;

import java.math.BigDecimal;
import java.time.Instant;

/** What the Payment Service tells the outside world about a payment. */
public record PaymentResponse(
        boolean success,
        String service,
        String paymentId,
        String sagaId,
        String orderId,
        String customerName,
        BigDecimal amount,
        String status,
        String transactionReference,
        String refundReference,
        String failureReason,
        Instant createdAt,
        Instant refundedAt
) {

    public static PaymentResponse from(PaymentEntity payment) {
        return new PaymentResponse(
                true,
                "payment-service",
                payment.getPaymentId(),
                payment.getSagaId(),
                payment.getOrderId(),
                payment.getCustomerName(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getTransactionReference(),
                payment.getRefundReference(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getRefundedAt()
        );
    }
}
