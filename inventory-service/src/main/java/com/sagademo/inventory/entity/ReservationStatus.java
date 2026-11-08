package com.sagademo.inventory.entity;

/**
 * Lifecycle of a stock reservation.
 *
 * <pre>
 *   RESERVED ---(RELEASE_INVENTORY = compensating transaction C2)---> RELEASED
 * </pre>
 *
 * RESERVED is the pending / "semantic lock" state. Storing the reservation as a row (instead
 * of only decrementing a counter) is what makes the compensation both safe and idempotent:
 * the release operation knows exactly how many units to give back, and it can detect that it
 * has already run.
 *
 * <p>If the Saga completes successfully the reservation simply stays RESERVED - the units are
 * sold. A production system would usually add a fifth forward step ("consume reservation")
 * that converts the reservation into a shipment; it is left out here so the demo keeps the
 * four steps the presentation talks about.
 */
public enum ReservationStatus {

    /** Units are held for this saga. */
    RESERVED,

    /** Compensated: the units were given back to available stock. */
    RELEASED
}
