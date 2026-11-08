package com.sagademo.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PAYMENT SERVICE - independent Spring Boot microservice (port 8083).
 *
 * <p>Responsibility: it owns payments and refunds, in its own Payment database.
 * NOTE: no real payment provider, no real card, no real money is involved anywhere. The
 * "charge" is a row in a local H2 database.
 *
 * <p>Role inside the Saga:
 * <ul>
 *   <li>{@code POST /api/payments/process} -> local transaction T3 (PROCESS_PAYMENT)</li>
 *   <li>{@code POST /api/payments/refund}  -> compensating transaction C3 (REFUND_PAYMENT)</li>
 * </ul>
 *
 * <p>Payment is the clearest example of why compensation is not a rollback: once money has
 * been captured, the only way back is a refund, which is a new business transaction that is
 * itself recorded in the ledger. The original charge is never erased.
 */
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
