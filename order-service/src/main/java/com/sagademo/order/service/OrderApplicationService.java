package com.sagademo.order.service;

import com.sagademo.order.dto.CreateOrderRequest;
import com.sagademo.order.dto.OrderActionRequest;
import com.sagademo.order.entity.OrderEntity;
import com.sagademo.order.entity.OrderStatus;
import com.sagademo.order.exception.InvalidOrderStateException;
import com.sagademo.order.exception.OrderNotFoundException;
import com.sagademo.order.exception.SimulatedFailureException;
import com.sagademo.order.repository.OrderRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ALL Order business logic lives here. The controller only maps HTTP to these methods.
 *
 * <p>Every public method below is ONE LOCAL TRANSACTION (single {@code @Transactional} on a
 * single database). There is no global/XA transaction anywhere in this project - that is
 * the whole point of the Saga pattern:
 *
 * <pre>
 *   createOrder()   = T1  (local transaction, COMMITTED immediately)
 *   confirmOrder()  = T4  (local transaction, COMMITTED immediately)
 *   cancelOrder()   = C1  (COMPENSATING transaction for T1 - a new committed transaction
 *                          that semantically undoes T1; it is NOT a database rollback)
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private static final String SERVICE_NAME = "ORDER_SERVICE";

    private final OrderRepository orderRepository;
    private final FailureSimulationService failureSimulation;

    /**
     * Simulated "work" so each remote call visibly takes time during the presentation.
     * It is real latency on a real HTTP call, not a frontend animation.
     */
    @Value("${demo.processing-delay-ms:150}")
    private long processingDelayMs;

    /** Readable order ids (ORDER-0001, ORDER-0002, ...). Single instance => an AtomicLong is enough. */
    private final AtomicLong sequence = new AtomicLong(0);

    @PostConstruct
    void initSequence() {
        long max = orderRepository.findMaxSequenceNo();
        sequence.set(max);
        log.info("{} started | order id sequence continues from {}", SERVICE_NAME, max);
    }

    // ------------------------------------------------------------------------------------
    // T1 - CREATE_ORDER (forward)
    // ------------------------------------------------------------------------------------

    /**
     * Local transaction T1: persist a new order in state {@link OrderStatus#CREATED}.
     *
     * <p>CREATED is a pending business state (a "semantic lock"): the row is committed, but
     * the order is not yet valid. Only CONFIRM_ORDER makes it real.
     */
    @Transactional
    public OrderEntity createOrder(CreateOrderRequest request) {
        logStep(request.getSagaId(), null, "CREATE_ORDER", "STARTED");
        simulateWork();

        if (failureSimulation.shouldFailCreateOrder(request.isSimulateFailure())) {
            logStep(request.getSagaId(), null, "CREATE_ORDER", "FAILED");
            throw new SimulatedFailureException(
                    "ORDER_CREATION_FAILED",
                    "Simulated failure while creating the order in order-service",
                    request.getSagaId());
        }

        long next = sequence.incrementAndGet();
        String orderId = "ORDER-%04d".formatted(next);
        BigDecimal totalAmount = request.getUnitPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        OrderEntity order = OrderEntity.builder()
                .orderId(orderId)
                .sagaId(request.getSagaId())
                .customerName(request.getCustomerName())
                .productId(request.getProductId())
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalAmount(totalAmount)
                .status(OrderStatus.CREATED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .sequenceNo(next)
                .build();

        OrderEntity saved = orderRepository.save(order);
        logStep(request.getSagaId(), orderId, "CREATE_ORDER", "COMPLETED");
        return saved;
    }

    // ------------------------------------------------------------------------------------
    // T4 - CONFIRM_ORDER (forward)
    // ------------------------------------------------------------------------------------

    /**
     * Local transaction T4: the last forward step of the Saga. Moves the order from the
     * pending state CREATED to the final state CONFIRMED.
     *
     * <p>If this step fails, THREE earlier local transactions are already committed in three
     * different databases (order created, inventory reserved, payment captured). That is
     * exactly why a Saga is necessary - see demo Scenario 4.
     */
    @Transactional
    public OrderEntity confirmOrder(String orderId, OrderActionRequest request) {
        logStep(request.getSagaId(), orderId, "CONFIRM_ORDER", "STARTED");
        simulateWork();

        OrderEntity order = findOrder(orderId);

        if (failureSimulation.shouldFailConfirmOrder(request.isSimulateFailure())) {
            logStep(request.getSagaId(), orderId, "CONFIRM_ORDER", "FAILED");
            throw new SimulatedFailureException(
                    "ORDER_CONFIRMATION_FAILED",
                    "Simulated failure while confirming order " + orderId,
                    request.getSagaId());
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException(
                    "Cannot confirm order " + orderId + " because it is already CANCELLED");
        }

        // Idempotent: confirming an already confirmed order is a no-op, not an error.
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);
        }

        logStep(request.getSagaId(), orderId, "CONFIRM_ORDER", "COMPLETED");
        return order;
    }

    // ------------------------------------------------------------------------------------
    // C1 - CANCEL_ORDER (compensation for T1)
    // ------------------------------------------------------------------------------------

    /**
     * COMPENSATING TRANSACTION for T1.
     *
     * <p>It cannot "roll back" T1 - that transaction was committed minutes ago and other
     * services may have read it. Instead it performs a new, forward-moving business
     * operation that makes the system consistent again: the order becomes CANCELLED.
     *
     * <p>Compensations must be IDEMPOTENT, because the orchestrator may retry them.
     * Cancelling an already-cancelled order therefore returns successfully.
     */
    @Transactional
    public OrderEntity cancelOrder(String orderId, OrderActionRequest request) {
        logStep(request.getSagaId(), orderId, "CANCEL_ORDER (compensation)", "STARTED");
        simulateWork();

        OrderEntity order = findOrder(orderId);

        if (failureSimulation.shouldFailCancelOrder(request.isSimulateFailure())) {
            logStep(request.getSagaId(), orderId, "CANCEL_ORDER (compensation)", "FAILED");
            throw new SimulatedFailureException(
                    "ORDER_CANCELLATION_FAILED",
                    "Simulated failure while cancelling order " + orderId
                            + " - the compensating transaction itself failed",
                    request.getSagaId());
        }

        if (order.getStatus() != OrderStatus.CANCELLED) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancellationReason(
                    request.getReason() == null ? "Cancelled by Saga compensation" : request.getReason());
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);
        }

        logStep(request.getSagaId(), orderId, "CANCEL_ORDER (compensation)", "COMPENSATED");
        return order;
    }

    // ------------------------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public OrderEntity findOrder(String orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> findAllOrders() {
        return orderRepository.findAllByOrderBySequenceNoDesc();
    }

    /** Demo helper: wipes the Order database so a presentation can start from a clean slate. */
    @Transactional
    public void resetAllData() {
        orderRepository.deleteAll();
        sequence.set(0);
        log.warn("{} | ORDER DATABASE RESET (demo helper)", SERVICE_NAME);
    }

    // ------------------------------------------------------------------------------------
    // Observability helper - requirement #17: one line per step, traceable across services
    // ------------------------------------------------------------------------------------

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
