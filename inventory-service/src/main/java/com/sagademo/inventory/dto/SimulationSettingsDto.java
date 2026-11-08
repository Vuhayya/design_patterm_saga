package com.sagademo.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sticky, service-level failure switches for the Inventory Service.
 * Effective decision for a step is {@code requestFlag || stickyFlag}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationSettingsDto {

    /** Make the RESERVE_INVENTORY local transaction fail (demo Scenario 3). */
    private boolean failReserveInventory;

    /** Make the RELEASE_INVENTORY *compensating* transaction fail (demo Scenario 5). */
    private boolean failReleaseInventory;
}
