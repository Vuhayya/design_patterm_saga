package com.sagademo.inventory.entity;

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

import java.time.Instant;

/**
 * One stock reservation made by one Saga. Lives ONLY in the Inventory database.
 *
 * <p>It is the audit trail of Saga step 2 and of its compensation, and it is what makes
 * RELEASE_INVENTORY idempotent (a second release finds the row already RELEASED).
 */
@Entity
@Table(name = "inventory_reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservation {

    /** Business primary key, e.g. {@code RES-0001}. */
    @Id
    @Column(name = "reservation_id", nullable = false, length = 40)
    private String reservationId;

    /** Correlation id of the distributed transaction. */
    @Column(name = "saga_id", nullable = false, length = 60)
    private String sagaId;

    @Column(name = "order_id", length = 40)
    private String orderId;

    @Column(name = "product_id", nullable = false, length = 60)
    private String productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    /** Set by the compensating transaction. */
    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "sequence_no", nullable = false)
    private Long sequenceNo;
}
