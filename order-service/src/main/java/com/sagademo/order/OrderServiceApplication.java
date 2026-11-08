package com.sagademo.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ORDER SERVICE - independent Spring Boot microservice (port 8081).
 *
 * <p>Responsibility (single bounded context): it owns the Order aggregate and the
 * Order database. Nobody else is allowed to read or write the ORDERS table.
 *
 * <p>Role inside the Saga:
 * <ul>
 *   <li>{@code POST /api/orders}                  -> local transaction T1 (CREATE_ORDER)</li>
 *   <li>{@code POST /api/orders/{id}/confirm}     -> local transaction T4 (CONFIRM_ORDER)</li>
 *   <li>{@code POST /api/orders/{id}/cancel}      -> compensating transaction C1 (CANCEL_ORDER)</li>
 * </ul>
 *
 * <p>Notice that the compensation is a normal, forward-moving business operation
 * ("cancel the order"), NOT a database rollback. That is the essence of the Saga
 * pattern: each local transaction is committed immediately, so undoing it later
 * requires a new semantically-inverse transaction.
 */
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
