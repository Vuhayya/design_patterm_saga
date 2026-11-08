package com.sagademo.order.entity;

/**
 * Lifecycle of an Order inside its own local database.
 *
 * <pre>
 *   CREATED ---(CONFIRM_ORDER)---> CONFIRMED
 *      |
 *      +------(CANCEL_ORDER, compensation)---> CANCELLED
 * </pre>
 *
 * CREATED is deliberately a "pending / not yet valid" business state. The order row
 * is committed to the Order DB immediately (that is the local transaction), but the
 * business meaning is "not confirmed yet". This is the classic Saga technique of
 * using a semantic lock / pending state so that an intermediate, not-yet-consistent
 * state is never mistaken for a finished order.
 */
public enum OrderStatus {

    /** Local transaction T1 committed. Order exists but is NOT valid for the kitchen yet. */
    CREATED,

    /** Local transaction T4 committed. The whole Saga succeeded - the order is real. */
    CONFIRMED,

    /** Compensating transaction C1 committed. The Saga failed and was rolled back semantically. */
    CANCELLED
}
