package com.sagademo.saga.saga.steps;

import com.sagademo.saga.client.InventoryClient;
import com.sagademo.saga.client.SagaCallContext;
import com.sagademo.saga.client.dto.ReleaseInventoryPayload;
import com.sagademo.saga.client.dto.ReservationClientResponse;
import com.sagademo.saga.client.dto.ReserveInventoryPayload;
import com.sagademo.saga.entity.SagaStepName;
import com.sagademo.saga.saga.SagaContext;
import com.sagademo.saga.saga.SagaStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * SAGA STEP 2 of 4.
 *
 * <pre>
 *   execute()     T2  POST /api/inventory/reserve  -> reservedUnits += qty   (available goes down)
 *   compensate()  C2  POST /api/inventory/release  -> reservedUnits -= qty   (available restored)
 * </pre>
 *
 * <p>This is the step whose compensation is easiest to verify on screen: the STATE COMPARISON
 * panel shows 100 -> 99 -> 100.
 */
@Component
@RequiredArgsConstructor
public class ReserveInventoryStep implements SagaStep {

    private final InventoryClient inventoryClient;

    @Override
    public SagaStepName name() {
        return SagaStepName.RESERVE_INVENTORY;
    }

    @Override
    public String targetService() {
        return InventoryClient.SERVICE_NAME;
    }

    @Override
    public void execute(SagaContext context) {
        ReserveInventoryPayload payload = new ReserveInventoryPayload(
                context.getSagaId(),
                context.getOrderId(),
                context.getProductId(),
                context.getQuantity(),
                context.getFailures().isFailReserveInventory());

        ReservationClientResponse response = inventoryClient.reserve(
                SagaCallContext.forward(context.getSagaId(), context.getOrderId(), name()), payload);

        context.setReservationId(response.reservationId());
    }

    @Override
    public void compensate(SagaContext context) {
        ReleaseInventoryPayload payload = new ReleaseInventoryPayload(
                context.getSagaId(),
                context.getReservationId(),
                context.getCompensationReason(),
                context.getFailures().isFailReleaseInventory());

        inventoryClient.release(
                SagaCallContext.compensation(context.getSagaId(), context.getOrderId(), compensationName()),
                payload);
    }
}
