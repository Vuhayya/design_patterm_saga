package com.sagademo.payment.service.impl;

import com.sagademo.payment.dto.ProcessPaymentRequest;
import com.sagademo.payment.dto.RefundPaymentRequest;
import com.sagademo.payment.entity.PaymentEntity;
import com.sagademo.payment.entity.PaymentStatus;
import com.sagademo.payment.exception.PaymentNotFoundException;
import com.sagademo.payment.exception.SimulatedFailureException;
import com.sagademo.payment.repository.PaymentRepository;
import com.sagademo.payment.service.FailureSimulationService;
import com.sagademo.payment.service.PaymentApplicationService;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ALL Payment business logic.
 *
 * <pre>
 *   processPayment()  = T3  (local transaction, COMMITTED immediately)
 *   refundPayment()   = C3  (COMPENSATING transaction for T3)
 * </pre>
 *
 * <p>NO real payment gateway is called and no real money moves. "Capturing" a payment means
 * inserting one row into this service's own H2 database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationServiceImpl implements PaymentApplicationService {

    private static final String SERVICE_NAME = "PAYMENT_SERVICE";

    private final PaymentRepository paymentRepository;
    private final FailureSimulationService failureSimulation;

    @Value("${demo.processing-delay-ms:200}")
    private long processingDelayMs;

    private final AtomicLong sequence = new AtomicLong(0);

    @PostConstruct
    void initSequence() {
        sequence.set(paymentRepository.findMaxSequenceNo());
    }

    // ------------------------------------------------------------------------------------
    // T3 - PROCESS_PAYMENT (forward)
    // ------------------------------------------------------------------------------------

    /**
     * Local transaction T3: capture the money for an order.
     *
     * <p>Deliberately NOT annotated with {@code @Transactional}. This method performs exactly
     * one committed write - either a COMPLETED payment or, on the failure path, a FAILED
     * payment kept as an audit record. {@code JpaRepository.save()} is itself transactional, so
     * each call is one local transaction. If the whole method were wrapped in one transaction,
     * throwing after writing the FAILED row would roll that audit row away, and the demo could
     * not show "the payment was attempted and rejected".
     *
     * <p>Idempotent by sagaId: a retry returns the payment that already exists for the saga
     * instead of charging the customer twice.
     */
    @Override
    public PaymentEntity processPayment(ProcessPaymentRequest request) {
        logStep(request.getSagaId(), request.getOrderId(), "PROCESS_PAYMENT", "STARTED");
        simulateWork();

        var existing = paymentRepository.findFirstBySagaIdOrderBySequenceNoDesc(request.getSagaId());
        if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.COMPLETED) {
            log.info("Payment for saga {} already COMPLETED ({}), returning it (idempotent)",
                    request.getSagaId(), existing.get().getPaymentId());
            return existing.get();
        }

        if (failureSimulation.shouldFailProcess(request.isSimulateFailure())) {
            String reason = "Lỗi giả lập: cổng thanh toán từ chối (declined) khoản thu %s cho đơn %s"
                    .formatted(request.getAmount(), request.getOrderId());

            // Audit row: the attempt really happened and really failed.
            paymentRepository.save(newPayment(request, PaymentStatus.FAILED, null, reason));

            logStep(request.getSagaId(), request.getOrderId(), "PROCESS_PAYMENT", "FAILED");
            throw new SimulatedFailureException("PAYMENT_FAILED", reason, request.getSagaId());
        }

        String transactionRef = "TXN-" + UUID.randomUUID().toString()
                .substring(0, 6).toUpperCase(Locale.ROOT);
        PaymentEntity payment = paymentRepository.save(
                newPayment(request, PaymentStatus.COMPLETED, transactionRef, null));

        log.info("Captured {} for order {} | transactionReference={}",
                payment.getAmount(), payment.getOrderId(), transactionRef);
        logStep(request.getSagaId(), request.getOrderId(), "PROCESS_PAYMENT", "COMPLETED");
        return payment;
    }

    // ------------------------------------------------------------------------------------
    // C3 - REFUND_PAYMENT (compensation for T3)
    // ------------------------------------------------------------------------------------

    /**
     * COMPENSATING TRANSACTION for T3.
     *
     * <p>This is the clearest illustration of the difference between compensation and rollback:
     * the original charge row is NOT deleted. A refund is appended, the payment moves to
     * REFUNDED, and the ledger keeps both facts forever. A database ROLLBACK, by contrast, would
     * behave as if the charge had never existed - which is impossible once the money has moved
     * and other systems have already seen it.
     *
     * <p>Idempotent: refunding an already REFUNDED payment is a successful no-op.
     */
    @Override
    @Transactional
    public PaymentEntity refundPayment(RefundPaymentRequest request) {
        logStep(request.getSagaId(), null, "REFUND_PAYMENT (compensation)", "STARTED");
        simulateWork();

        if (failureSimulation.shouldFailRefund(request.isSimulateFailure())) {
            logStep(request.getSagaId(), null, "REFUND_PAYMENT (compensation)", "FAILED");
            throw new SimulatedFailureException(
                    "PAYMENT_REFUND_FAILED",
                    "Lỗi giả lập khi hoàn tiền của saga " + request.getSagaId()
                            + " - chính compensating transaction bị lỗi",
                    request.getSagaId());
        }

        PaymentEntity payment = resolvePayment(request);

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.info("Payment {} is already REFUNDED - nothing to do (idempotent compensation)",
                    payment.getPaymentId());
            return payment;
        }

        if (payment.getStatus() == PaymentStatus.FAILED) {
            // Nothing was ever captured, so there is nothing to give back.
            log.info("Payment {} is FAILED - no money was captured, refund is a no-op",
                    payment.getPaymentId());
            return payment;
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundReference("RFD-" + UUID.randomUUID().toString()
                .substring(0, 6).toUpperCase(Locale.ROOT));
        payment.setFailureReason(request.getReason());
        payment.setRefundedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);

        log.info("Refunded {} for order {} | refundReference={}",
                payment.getAmount(), payment.getOrderId(), payment.getRefundReference());
        logStep(request.getSagaId(), payment.getOrderId(), "REFUND_PAYMENT (compensation)", "COMPENSATED");
        return payment;
    }

    // ------------------------------------------------------------------------------------
    // Queries / demo helpers
    // ------------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PaymentEntity findPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Unknown payment: " + paymentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentEntity> findAllPayments() {
        return paymentRepository.findAllByOrderBySequenceNoDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentEntity findBySagaId(String sagaId) {
        return paymentRepository.findFirstBySagaIdOrderBySequenceNoDesc(sagaId)
                .orElseThrow(() -> new PaymentNotFoundException("No payment for saga: " + sagaId));
    }

    @Override
    @Transactional
    public void resetAllData() {
        paymentRepository.deleteAll();
        sequence.set(0);
        log.warn("{} | PAYMENT DATABASE RESET (demo helper)", SERVICE_NAME);
    }

    private PaymentEntity newPayment(ProcessPaymentRequest request,
                                     PaymentStatus status,
                                     String transactionRef,
                                     String failureReason) {
        long next = sequence.incrementAndGet();
        return PaymentEntity.builder()
                .paymentId("PAY-%04d".formatted(next))
                .sagaId(request.getSagaId())
                .orderId(request.getOrderId())
                .customerName(request.getCustomerName())
                .amount(request.getAmount())
                .status(status)
                .transactionReference(transactionRef)
                .failureReason(failureReason)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .sequenceNo(next)
                .build();
    }

    private PaymentEntity resolvePayment(RefundPaymentRequest request) {
        if (request.getPaymentId() != null && !request.getPaymentId().isBlank()) {
            return findPayment(request.getPaymentId());
        }
        return findBySagaId(request.getSagaId());
    }

    private void logStep(String sagaId, String orderId, String stepName, String status) {
        log.info("[{}] [{}] [{}] {} {}",
                sagaId == null ? "no-saga" : sagaId,
                orderId == null ? "no-order" : orderId,
                SERVICE_NAME, stepName, status);
    }

    private void simulateWork() {
        if (processingDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(processingDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
