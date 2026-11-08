package com.sagademo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sticky, service-level failure switches for the Order Service.
 *
 * <p>There are two independent ways to make a service fail in this demo:
 * <ol>
 *   <li><b>Per request</b> - {@code simulateFailure} inside the step payload
 *       (this is what the "Failure Simulation" checkboxes on the order form use).</li>
 *   <li><b>Sticky, per service</b> - these switches, toggled via
 *       {@code POST /api/orders/simulation}. They stay on until you turn them off,
 *       which is handy for showing "this service is currently broken" on the dashboard.</li>
 * </ol>
 * The effective decision is {@code requestFlag || stickyFlag}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationSettingsDto {

    /** Make the CREATE_ORDER local transaction fail. */
    private boolean failCreateOrder;

    /** Make the CONFIRM_ORDER local transaction fail (demo Scenario 4). */
    private boolean failConfirmOrder;

    /** Make the CANCEL_ORDER *compensating* transaction fail (demo Scenario 5 - compensation failure). */
    private boolean failCancelOrder;
}
