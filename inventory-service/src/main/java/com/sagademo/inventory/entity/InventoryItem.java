package com.sagademo.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Stock of one product. Lives ONLY in the Inventory database.
 *
 * <pre>
 *   availableUnits = totalUnits - reservedUnits
 * </pre>
 *
 * RESERVE_INVENTORY  : reservedUnits += qty     (available goes down)
 * RELEASE_INVENTORY  : reservedUnits -= qty     (available goes back up)  <-- compensation
 *
 * This is what the "STATE COMPARISON" panel in the UI reads, and it is why the
 * before/after comparison proves that compensation really restored the state.
 */
@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

    @Id
    @Column(name = "product_id", nullable = false, length = 60)
    private String productId;

    @Column(name = "product_name", nullable = false, length = 120)
    private String productName;

    /** Physical stock on the shelf. */
    @Column(name = "total_units", nullable = false)
    private Integer totalUnits;

    /** Units held by in-flight or confirmed sagas. */
    @Column(name = "reserved_units", nullable = false)
    private Integer reservedUnits;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Optimistic locking: protects the stock count when two sagas run concurrently. */
    @Version
    @Column(name = "version")
    private Long version;

    /** Derived value - never stored, always computed, so it can never drift. */
    public int getAvailableUnits() {
        return totalUnits - reservedUnits;
    }
}
