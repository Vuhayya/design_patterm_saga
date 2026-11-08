package com.sagademo.payment.service;

import com.sagademo.payment.dto.ProcessPaymentRequest;
import com.sagademo.payment.dto.RefundPaymentRequest;
import com.sagademo.payment.entity.PaymentEntity;
import com.sagademo.payment.entity.PaymentStatus;
import com.sagademo.payment.exception.SimulatedFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Local-transaction tests for the Payment Service (T3 and its compensation C3). */
@SpringBootTest
class PaymentApplicationServiceTest {

    @Autowired
    private PaymentApplicationService paymentService;

    @Autowired
    private FailureSimulationService failureSimulation;

    @BeforeEach
    void reset() {
        paymentService.resetAllData();
        failureSimulation.reset();
    }

    private ProcessPaymentRequest processRequest(String sagaId, boolean fail) {
        return ProcessPaymentRequest.builder()
                .sagaId(sagaId)
                .orderId("ORDER-0001")
                .customerName("Nguyen Van A")
                .amount(new BigDecimal("240000"))
                .simulateFailure(fail)
                .build();
    }

    @Test
    @DisplayName("T3 PROCESS_PAYMENT captures the money and produces a transaction reference")
    void processCapturesMoney() {
        PaymentEntity payment = paymentService.processPayment(processRequest("SAGA-1", false));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getTransactionReference()).startsWith("TXN-");
        assertThat(payment.getAmount()).isEqualByComparingTo("240000");
    }

    @Test
    @DisplayName("T3 really fails when simulated, and keeps a FAILED audit row (scenario 2)")
    void processFailsAndKeepsAuditRow() {
        assertThatThrownBy(() -> paymentService.processPayment(processRequest("SAGA-2", true)))
                .isInstanceOf(SimulatedFailureException.class);

        PaymentEntity failed = paymentService.findBySagaId("SAGA-2");
        assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failed.getTransactionReference()).isNull();
        assertThat(failed.getFailureReason()).contains("declined");
    }

    @Test
    @DisplayName("C3 REFUND_PAYMENT reverses a captured payment without deleting it")
    void refundReversesWithoutDeleting() {
        PaymentEntity payment = paymentService.processPayment(processRequest("SAGA-3", false));

        PaymentEntity refunded = paymentService.refundPayment(RefundPaymentRequest.builder()
                .sagaId("SAGA-3")
                .paymentId(payment.getPaymentId())
                .reason("order confirmation failed")
                .build());

        assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(refunded.getRefundReference()).startsWith("RFD-");
        // The original charge row still exists - compensation appends, it does not erase.
        assertThat(paymentService.findPayment(payment.getPaymentId()).getTransactionReference())
                .isEqualTo(payment.getTransactionReference());
    }

    @Test
    @DisplayName("C3 can compensate using only the sagaId")
    void refundBySagaIdOnly() {
        paymentService.processPayment(processRequest("SAGA-4", false));

        PaymentEntity refunded = paymentService.refundPayment(
                RefundPaymentRequest.builder().sagaId("SAGA-4").build());

        assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("C3 is idempotent - a retried refund must not refund twice")
    void refundIsIdempotent() {
        paymentService.processPayment(processRequest("SAGA-5", false));
        RefundPaymentRequest refund = RefundPaymentRequest.builder().sagaId("SAGA-5").build();

        PaymentEntity first = paymentService.refundPayment(refund);
        PaymentEntity second = paymentService.refundPayment(refund);

        assertThat(second.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(second.getRefundReference()).isEqualTo(first.getRefundReference());
    }

    @Test
    @DisplayName("C3 itself can fail (scenario 5) and then the money stays captured")
    void refundCanFail() {
        paymentService.processPayment(processRequest("SAGA-6", false));

        assertThatThrownBy(() -> paymentService.refundPayment(RefundPaymentRequest.builder()
                .sagaId("SAGA-6").simulateFailure(true).build()))
                .isInstanceOf(SimulatedFailureException.class);

        assertThat(paymentService.findBySagaId("SAGA-6").getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("T3 is idempotent per sagaId - a retry must not charge the customer twice")
    void processIsIdempotentPerSaga() {
        PaymentEntity first = paymentService.processPayment(processRequest("SAGA-7", false));
        PaymentEntity second = paymentService.processPayment(processRequest("SAGA-7", false));

        assertThat(second.getPaymentId()).isEqualTo(first.getPaymentId());
        assertThat(paymentService.findAllPayments()).hasSize(1);
    }
}
