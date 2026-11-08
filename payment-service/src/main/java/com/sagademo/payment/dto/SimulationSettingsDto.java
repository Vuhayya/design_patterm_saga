package com.sagademo.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sticky, service-level failure switches for the Payment Service.
 * Effective decision for a step is {@code requestFlag || stickyFlag}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationSettingsDto {

    /** Make the PROCESS_PAYMENT local transaction fail (demo Scenario 2 - the headline demo). */
    private boolean failProcessPayment;

    /** Make the REFUND_PAYMENT *compensating* transaction fail (demo Scenario 5). */
    private boolean failRefundPayment;
}
