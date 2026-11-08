package com.sagademo.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The Order aggregate. Lives ONLY in the Order Service database (ORDER DB).
 *
 * <p>{@code sagaId} is stored here on purpose: it is the correlation id that lets you
 * trace one distributed business transaction across five independent databases.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {

    /** Business primary key, e.g. {@code ORDER-0001}. Readable ids make the demo logs easy to follow. */
    @Id
    @Column(name = "order_id", nullable = false, length = 40)
    private String orderId;

    /** Correlation id of the Saga that created this order. Null is not expected in this demo. */
    @Column(name = "saga_id", length = 60)
    private String sagaId;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "product_id", nullable = false, length = 60)
    private String productId;

    @Column(name = "product_name", length = 120)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    /** Filled in by the compensating transaction so the audit trail explains *why* it was cancelled. */
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Monotonic sequence behind the readable order id, used to generate the next id after a restart. */
    @Column(name = "sequence_no", nullable = false)
    private Long sequenceNo;
}
