package com.sagademo.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * INVENTORY SERVICE - independent Spring Boot microservice (port 8082).
 *
 * <p>Responsibility: it owns stock levels and stock reservations, in its own Inventory
 * database. The Order Service cannot see this data and vice versa.
 *
 * <p>Role inside the Saga:
 * <ul>
 *   <li>{@code POST /api/inventory/reserve} -> local transaction T2 (RESERVE_INVENTORY)</li>
 *   <li>{@code POST /api/inventory/release} -> compensating transaction C2 (RELEASE_INVENTORY)</li>
 * </ul>
 *
 * <p>Note the modelling choice: reserving does NOT decrement total stock, it increments
 * {@code reservedUnits}. "Reserved" is a pending state - the Saga equivalent of a lock that
 * can be undone by a later business operation rather than by a database rollback.
 */
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
