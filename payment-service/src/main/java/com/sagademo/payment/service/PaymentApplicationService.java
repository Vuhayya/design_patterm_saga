package com.sagademo.payment.service;

import com.sagademo.payment.dto.ProcessPaymentRequest;
import com.sagademo.payment.dto.RefundPaymentRequest;
import com.sagademo.payment.entity.PaymentEntity;
import java.util.List;

/**
 * ALL Payment business logic.
 *
 * <p>Contract only. Controllers and other services depend on this interface, never on the
 * implementation class.
 *
 * @see com.sagademo.payment.service.impl.PaymentApplicationServiceImpl
 */
public interface PaymentApplicationService {

    /** Local transaction T3: capture the money for an order. */
    PaymentEntity processPayment(ProcessPaymentRequest request);

    /** COMPENSATING TRANSACTION for T3. */
    PaymentEntity refundPayment(RefundPaymentRequest request);

    PaymentEntity findPayment(String paymentId);

    List<PaymentEntity> findAllPayments();

    PaymentEntity findBySagaId(String sagaId);

    void resetAllData();
}
