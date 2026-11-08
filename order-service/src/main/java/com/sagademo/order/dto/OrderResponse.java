package com.sagademo.order.dto;

import com.sagademo.order.entity.OrderEntity;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * What the Order Service tells the outside world about an order.
 * Entities are never exposed directly - the API contract is a DTO.
 */
public record OrderResponse(
        boolean success,
        String service,
        String orderId,
        String sagaId,
        String customerName,
        String productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String status,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt
) {

    public static OrderResponse from(OrderEntity order) {
        return new OrderResponse(
                true,
                "order-service",
                order.getOrderId(),
                order.getSagaId(),
                order.getCustomerName(),
                order.getProductId(),
                order.getProductName(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getCancellationReason(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
