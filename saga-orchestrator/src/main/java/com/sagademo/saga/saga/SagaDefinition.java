package com.sagademo.saga.saga;

import com.sagademo.saga.saga.steps.ConfirmOrderStep;
import com.sagademo.saga.saga.steps.CreateOrderStep;
import com.sagademo.saga.saga.steps.ProcessPaymentStep;
import com.sagademo.saga.saga.steps.ReserveInventoryStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * THE SAGA, DECLARED IN ONE PLACE.
 *
 * <p>The order of this list IS the business transaction:
 *
 * <pre>
 *   1. CREATE_ORDER        (order-service)
 *   2. RESERVE_INVENTORY   (inventory-service)
 *   3. PROCESS_PAYMENT     (payment-service)
 *   4. CONFIRM_ORDER       (order-service)
 * </pre>
 *
 * <p>The compensation order is simply this list REVERSED, and the orchestrator derives it
 * automatically - there is no second list to keep in sync. Reverse order matters: you refund the
 * money before you give the stock back, exactly as you would unwind a stack.
 */
@Component
@RequiredArgsConstructor
public class SagaDefinition {

    private final CreateOrderStep createOrderStep;
    private final ReserveInventoryStep reserveInventoryStep;
    private final ProcessPaymentStep processPaymentStep;
    private final ConfirmOrderStep confirmOrderStep;

    /** The forward steps, in execution order. */
    public List<SagaStep> steps() {
        return List.of(createOrderStep, reserveInventoryStep, processPaymentStep, confirmOrderStep);
    }
}
