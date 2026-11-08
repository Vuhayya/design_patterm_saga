package com.sagademo.inventory.exception;

/**
 * A genuine (non-simulated) business failure: not enough available units.
 *
 * <p>Worth pointing out during the presentation - the Saga does not only have to handle
 * crashes and timeouts, it must also handle legitimate business rejections, and both lead
 * to exactly the same compensation flow.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productId, int requested, int available) {
        super("Insufficient stock for product %s: requested %d, available %d"
                .formatted(productId, requested, available));
    }
}
