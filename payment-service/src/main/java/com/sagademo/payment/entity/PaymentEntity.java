package com.sagademo.payment.entity;

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

/** A payment (or a failed payment attempt). Lives ONLY in the Payment database. */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity {

    /** Business primary key, e.g. {@code PAY-0001}. */
    @Id
    @Column(name = "payment_id", nullable = false, length = 40)
    private String paymentId;

    @Column(name = "saga_id", nullable = false, length = 60)
    private String sagaId;

    @Column(name = "order_id", length = 40)
    private String orderId;

    @Column(name = "customer_name", length = 120)
    private String customerName;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    /** Fake gateway reference, e.g. {@code TXN-8F3A21}. Shown in the demo UI. */
    @Column(name = "transaction_reference", length = 60)
    private String transactionReference;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Filled in by the compensating transaction C3. */
    @Column(name = "refund_reference", length = 60)
    private String refundReference;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "sequence_no", nullable = false)
    private Long sequenceNo;
}
