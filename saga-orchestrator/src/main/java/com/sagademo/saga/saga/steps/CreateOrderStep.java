package com.sagademo.saga.saga.steps;

import com.sagademo.saga.client.OrderClient;
import com.sagademo.saga.client.SagaCallContext;
import com.sagademo.saga.client.dto.CreateOrderPayload;
import com.sagademo.saga.client.dto.OrderActionPayload;
import com.sagademo.saga.client.dto.OrderClientResponse;
import com.sagademo.saga.entity.SagaStepName;
import com.sagademo.saga.saga.SagaContext;
import com.sagademo.saga.saga.SagaStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * SAGA STEP 1 of 4.
 *
 * <pre>
 *   execute()     T1  POST /api/orders                  -> order becomes CREATED  (pending state)
 *   compensate()  C1  POST /api/orders/{id}/cancel      -> order becomes CANCELLED
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class CreateOrderStep implements SagaStep {

    private final OrderClient orderClient;

    @Override
    public SagaStepName name() {
        return SagaStepName.CREATE_ORDER;
    }

    @Override
    public String targetService() {
        return OrderClient.SERVICE_NAME;
    }

    @Override
    public void execute(SagaContext context) {
        CreateOrderPayload payload = new CreateOrderPayload(
                context.getSagaId(),
                context.getCustomerName(),
                context.getProductId(),
                context.getProductName(),
                context.getQuantity(),
                context.getUnitPrice(),
                context.getFailures().isFailCreateOrder());

        OrderClientResponse response = orderClient.createOrder(
                SagaCallContext.forward(context.getSagaId(), null, name()), payload);

        // Hand the new orderId to every later step AND to the compensation of this step.
        context.setOrderId(response.orderId());
    }

    @Override
    public void compensate(SagaContext context) {
        if (context.getOrderId() == null) {
            // Nothing was created, so there is nothing to cancel.
            return;
        }
        OrderActionPayload payload = new OrderActionPayload(
                context.getSagaId(),
                context.getCompensationReason(),
                context.getFailures().isFailCancelOrder());

        orderClient.cancelOrder(
                SagaCallContext.compensation(context.getSagaId(), context.getOrderId(), compensationName()),
                context.getOrderId(), payload);
    }
}
