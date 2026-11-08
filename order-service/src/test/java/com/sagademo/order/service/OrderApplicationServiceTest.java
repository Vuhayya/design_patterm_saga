package com.sagademo.order.service;

import com.sagademo.order.dto.CreateOrderRequest;
import com.sagademo.order.dto.OrderActionRequest;
import com.sagademo.order.entity.OrderEntity;
import com.sagademo.order.entity.OrderStatus;
import com.sagademo.order.exception.InvalidOrderStateException;
import com.sagademo.order.exception.SimulatedFailureException;
import com.sagademo.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Local-transaction tests for the Order Service (T1, T4 and the compensation C1).
 */
@SpringBootTest
class OrderApplicationServiceTest {

    @Autowired
    private OrderApplicationService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private FailureSimulationService failureSimulation;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        failureSimulation.reset();
    }

    private CreateOrderRequest createRequest(boolean fail) {
        return CreateOrderRequest.builder()
                .sagaId("SAGA-TEST-1")
                .customerName("Nguyen Van A")
                .productId("PIZZA-01")
                .productName("Pepperoni Pizza")
                .quantity(2)
                .unitPrice(new BigDecimal("120000"))
                .simulateFailure(fail)
                .build();
    }

    @Test
    @DisplayName("T1 CREATE_ORDER commits the order in the pending state CREATED")
    void createOrderPersistsPendingOrder() {
        OrderEntity order = orderService.createOrder(createRequest(false));

        assertThat(order.getOrderId()).startsWith("ORDER-");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("240000");
        assertThat(orderRepository.findById(order.getOrderId())).isPresent();
    }

    @Test
    @DisplayName("T1 CREATE_ORDER really fails (and commits nothing) when failure is simulated")
    void createOrderFailsAndPersistsNothing() {
        assertThatThrownBy(() -> orderService.createOrder(createRequest(true)))
                .isInstanceOf(SimulatedFailureException.class);

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    @DisplayName("T4 CONFIRM_ORDER moves CREATED -> CONFIRMED")
    void confirmOrderMovesToConfirmed() {
        OrderEntity order = orderService.createOrder(createRequest(false));

        OrderEntity confirmed = orderService.confirmOrder(order.getOrderId(),
                OrderActionRequest.builder().sagaId("SAGA-TEST-1").build());

        assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("T4 CONFIRM_ORDER really fails when failure is simulated (scenario 4 trigger)")
    void confirmOrderCanFail() {
        OrderEntity order = orderService.createOrder(createRequest(false));

        assertThatThrownBy(() -> orderService.confirmOrder(order.getOrderId(),
                OrderActionRequest.builder().sagaId("SAGA-TEST-1").simulateFailure(true).build()))
                .isInstanceOf(SimulatedFailureException.class);

        assertThat(orderService.findOrder(order.getOrderId()).getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    @DisplayName("C1 CANCEL_ORDER compensates T1 by moving the order to CANCELLED")
    void cancelOrderCompensatesCreation() {
        OrderEntity order = orderService.createOrder(createRequest(false));

        OrderEntity cancelled = orderService.cancelOrder(order.getOrderId(),
                OrderActionRequest.builder().sagaId("SAGA-TEST-1").reason("payment failed").build());

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.getCancellationReason()).isEqualTo("payment failed");
    }

    @Test
    @DisplayName("C1 CANCEL_ORDER is idempotent - a retried compensation must not fail")
    void cancelOrderIsIdempotent() {
        OrderEntity order = orderService.createOrder(createRequest(false));
        OrderActionRequest cancel = OrderActionRequest.builder().sagaId("SAGA-TEST-1").reason("retry").build();

        orderService.cancelOrder(order.getOrderId(), cancel);
        OrderEntity secondAttempt = orderService.cancelOrder(order.getOrderId(), cancel);

        assertThat(secondAttempt.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("A cancelled order can never be confirmed afterwards")
    void cannotConfirmCancelledOrder() {
        OrderEntity order = orderService.createOrder(createRequest(false));
        orderService.cancelOrder(order.getOrderId(),
                OrderActionRequest.builder().sagaId("SAGA-TEST-1").build());

        assertThatThrownBy(() -> orderService.confirmOrder(order.getOrderId(),
                OrderActionRequest.builder().sagaId("SAGA-TEST-1").build()))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    @DisplayName("Sticky service-level failure switch also makes the step fail")
    void stickyFailureSwitchWorks() {
        failureSimulation.update(com.sagademo.order.dto.SimulationSettingsDto.builder()
                .failCreateOrder(true).build());

        assertThatThrownBy(() -> orderService.createOrder(createRequest(false)))
                .isInstanceOf(SimulatedFailureException.class);
    }
}
