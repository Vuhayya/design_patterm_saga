package com.sagademo.payment.entity;

/**
 * Lifecycle of a payment.
 *
 * <pre>
 *   COMPLETED ---(REFUND_PAYMENT = compensating transaction C3)---> REFUNDED
 *   FAILED  (the charge never succeeded - nothing to compensate)
 * </pre>
 *
 * A FAILED payment is recorded as a row too. That is intentional: even a failed step leaves
 * an audit trail in its own database, which is what you want when you have to explain after
 * the fact why a distributed transaction was compensated.
 */
public enum PaymentStatus {

    /** The charge succeeded and the money is captured. */
    COMPLETED,

    /** The charge did not succeed. There is nothing to refund. */
    FAILED,

    /** The charge succeeded earlier and has now been reversed by the compensation. */
    REFUNDED
}
