package com.sagademo.saga.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * THE FAILURE SIMULATION SWITCHES (requirement #5 and #28).
 *
 * <p>The frontend sends this object together with the order. The orchestrator then forwards
 * each individual flag to the service that owns the corresponding step, which makes that
 * service genuinely throw. Nothing is faked in the frontend and nothing is short-circuited in
 * the orchestrator - the failure happens in the remote service, over HTTP.
 *
 * <pre>
 *   ---- first level: a FORWARD step fails -> compensation runs ----
 *   failCreateOrder        -> order-service     rejects  POST /api/orders
 *   failReserveInventory   -> inventory-service rejects  POST /api/inventory/reserve
 *   failProcessPayment     -> payment-service   rejects  POST /api/payments/process
 *   failConfirmOrder       -> order-service     rejects  POST /api/orders/{id}/confirm
 *
 *   ---- second level: a COMPENSATION fails -> SAGA COMPENSATION FAILED ----
 *   failReleaseInventory   -> inventory-service rejects  POST /api/inventory/release
 *   failRefundPayment      -> payment-service   rejects  POST /api/payments/refund
 *   failCancelOrder        -> order-service     rejects  POST /api/orders/{id}/cancel
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailureSimulationDto {

    // ---- level 1: forward steps ---------------------------------------------------------
    private boolean failCreateOrder;
    private boolean failReserveInventory;
    private boolean failProcessPayment;
    private boolean failConfirmOrder;

    // ---- level 2: compensating transactions ---------------------------------------------
    private boolean failReleaseInventory;
    private boolean failRefundPayment;
    private boolean failCancelOrder;

    public boolean anyForwardFailure() {
        return failCreateOrder || failReserveInventory || failProcessPayment || failConfirmOrder;
    }

    public boolean anyCompensationFailure() {
        return failReleaseInventory || failRefundPayment || failCancelOrder;
    }
}
