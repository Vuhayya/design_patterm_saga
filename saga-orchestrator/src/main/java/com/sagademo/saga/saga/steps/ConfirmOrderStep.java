package com.sagademo.saga.saga.steps;

import com.sagademo.saga.client.OrderClient;
import com.sagademo.saga.client.SagaCallContext;
import com.sagademo.saga.client.dto.OrderActionPayload;
import com.sagademo.saga.entity.SagaStepName;
import com.sagademo.saga.saga.SagaContext;
import com.sagademo.saga.saga.SagaStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * SAGA STEP 4 of 4 - the last forward step.
 *
 * <pre>
 *   execute()     T4  POST /api/orders/{id}/confirm -> order becomes CONFIRMED, SAGA COMPLETED
 *   compensate()  ---  none (CompensationAction.NONE)
 * </pre>
 *
 * <p>THIS IS THE MOST IMPORTANT STEP FOR THE PRESENTATION (demo Scenario 4). When it fails,
 * three local transactions in three different databases have ALREADY been committed:
 *
 * <pre>
 *   order-service     : order row exists (CREATED)
 *   inventory-service : 1 unit is held
 *   payment-service   : the customer has been charged
 * </pre>
 *
 * No database can roll those back - they are three separate databases and three separate,
 * already-committed transactions. The only way back is the Saga: REFUND_PAYMENT, then
 * RELEASE_INVENTORY, then CANCEL_ORDER.
 *
 * <p>The step itself has no compensation because if it fails it committed nothing of its own,
 * and if it succeeds the whole Saga succeeded.
 */
@Component
@RequiredArgsConstructor
public class ConfirmOrderStep implements SagaStep {

    private final OrderClient orderClient;

    @Override
    public SagaStepName name() {
        return SagaStepName.CONFIRM_ORDER;
    }

    @Override
    public String targetService() {
        return OrderClient.SERVICE_NAME;
    }

    @Override
    public void execute(SagaContext context) {
        OrderActionPayload payload = new OrderActionPayload(
                context.getSagaId(),
                "Confirmed by saga " + context.getSagaId(),
                context.getFailures().isFailConfirmOrder());

        orderClient.confirmOrder(
                SagaCallContext.forward(context.getSagaId(), context.getOrderId(), name()),
                context.getOrderId(), payload);
    }

    @Override
    public void compensate(SagaContext context) {
        throw new UnsupportedOperationException(
                "CONFIRM_ORDER has no compensating transaction - see CompensationAction.NONE. "
                        + "The orchestrator never calls this method because hasCompensation() is false.");
    }
}
