package com.sagademo.saga.entity;

/**
 * Every step this Saga can execute - the four forward local transactions and the three
 * compensating transactions.
 *
 * <pre>
 *   FORWARD                       COMPENSATION
 *   -------------------------     -------------------------
 *   CREATE_ORDER              ->  CANCEL_ORDER
 *   RESERVE_INVENTORY         ->  RELEASE_INVENTORY
 *   PROCESS_PAYMENT           ->  REFUND_PAYMENT
 *   CONFIRM_ORDER             ->  (none - see CompensationAction.NONE)
 * </pre>
 */
public enum SagaStepName {

    // ---- forward local transactions -------------------------------------------------
    CREATE_ORDER,
    RESERVE_INVENTORY,
    PROCESS_PAYMENT,
    CONFIRM_ORDER,

    // ---- compensating transactions --------------------------------------------------
    CANCEL_ORDER,
    RELEASE_INVENTORY,
    REFUND_PAYMENT
}
