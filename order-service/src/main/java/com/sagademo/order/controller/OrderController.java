package com.sagademo.order.controller;

import com.sagademo.order.dto.CreateOrderRequest;
import com.sagademo.order.dto.OrderActionRequest;
import com.sagademo.order.dto.OrderResponse;
import com.sagademo.order.service.OrderApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * THIN controller: it validates input, delegates to the service and maps the result to a DTO.
 * No business logic, no repository access.
 *
 * <pre>
 *   Controller  ->  Service  ->  Repository
 * </pre>
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderApplicationService orderService;

    /** Saga step 1 (CREATE_ORDER) - local transaction T1. */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderResponse.from(orderService.createOrder(request)));
    }

    /** Saga step 4 (CONFIRM_ORDER) - local transaction T4. */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable String orderId,
                                                      @Valid @RequestBody OrderActionRequest request) {
        return ResponseEntity.ok(OrderResponse.from(orderService.confirmOrder(orderId, request)));
    }

    /** COMPENSATION for step 1 (CANCEL_ORDER) - compensating transaction C1. */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String orderId,
                                                     @Valid @RequestBody OrderActionRequest request) {
        return ResponseEntity.ok(OrderResponse.from(orderService.cancelOrder(orderId, request)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(OrderResponse.from(orderService.findOrder(orderId)));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders() {
        return ResponseEntity.ok(orderService.findAllOrders().stream().map(OrderResponse::from).toList());
    }

    /** Demo helper: clears the Order database between presentation runs. */
    @DeleteMapping("/reset")
    public ResponseEntity<Void> reset() {
        orderService.resetAllData();
        return ResponseEntity.noContent().build();
    }
}
