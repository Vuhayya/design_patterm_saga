package com.sagademo.saga.saga;

import com.sagademo.saga.dto.FailureSimulationDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * THE SHARED STATE OF ONE RUNNING SAGA, passed from step to step.
 *
 * <p>Each forward step writes into it the id of the resource it created in a remote service
 * ({@code orderId}, {@code reservationId}, {@code paymentId}). The compensating steps read those
 * ids back to know exactly what to undo. That handover is the whole trick of orchestration:
 *
 * <pre>
 *   CREATE_ORDER      -> writes orderId        -> CANCEL_ORDER      reads orderId
 *   RESERVE_INVENTORY -> writes reservationId  -> RELEASE_INVENTORY reads reservationId
 *   PROCESS_PAYMENT   -> writes paymentId      -> REFUND_PAYMENT    reads paymentId
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaContext {

    /** Correlation id of this distributed transaction. */
    private String sagaId;

    // ---- business payload --------------------------------------------------------------
    private String customerName;
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;

    // ---- ids produced by the forward steps, consumed by the compensations ---------------
    private String orderId;
    private String reservationId;
    private String paymentId;

    /** Which steps the demo operator asked to fail. */
    private FailureSimulationDto failures;

    /** Why the saga is being compensated - forwarded to the services for their audit trail. */
    private String compensationReason;
}
