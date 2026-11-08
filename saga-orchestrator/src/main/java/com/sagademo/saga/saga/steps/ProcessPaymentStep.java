package com.sagademo.saga.saga.steps;

import com.sagademo.saga.client.PaymentClient;
import com.sagademo.saga.client.SagaCallContext;
import com.sagademo.saga.client.dto.PaymentClientResponse;
import com.sagademo.saga.client.dto.ProcessPaymentPayload;
import com.sagademo.saga.client.dto.RefundPaymentPayload;
import com.sagademo.saga.entity.SagaStepName;
import com.sagademo.saga.saga.SagaContext;
import com.sagademo.saga.saga.SagaStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * SAGA STEP 3 of 4.
 *
 * <pre>
 *   execute()     T3  POST /api/payments/process -> payment becomes COMPLETED
 *   compensate()  C3  POST /api/payments/refund  -> payment becomes REFUNDED (charge row kept)
 * </pre>
 *
 * <p>The best step to use when explaining why compensation is not a rollback: you cannot
 * "un-charge" a card, you can only issue a refund, and the ledger keeps both entries.
 */
@Component
@RequiredArgsConstructor
public class ProcessPaymentStep implements SagaStep {

    private final PaymentClient paymentClient;

    @Override
    public SagaStepName name() {
        return SagaStepName.PROCESS_PAYMENT;
    }

    @Override
    public String targetService() {
        return PaymentClient.SERVICE_NAME;
    }

    @Override
    public void execute(SagaContext context) {
        ProcessPaymentPayload payload = new ProcessPaymentPayload(
                context.getSagaId(),
                context.getOrderId(),
                context.getCustomerName(),
                context.getTotalAmount(),
                context.getFailures().isFailProcessPayment());

        PaymentClientResponse response = paymentClient.process(
                SagaCallContext.forward(context.getSagaId(), context.getOrderId(), name()), payload);

        context.setPaymentId(response.paymentId());
    }

    @Override
    public void compensate(SagaContext context) {
        RefundPaymentPayload payload = new RefundPaymentPayload(
                context.getSagaId(),
                context.getPaymentId(),
                context.getCompensationReason(),
                context.getFailures().isFailRefundPayment());

        paymentClient.refund(
                SagaCallContext.compensation(context.getSagaId(), context.getOrderId(), compensationName()),
                payload);
    }
}
