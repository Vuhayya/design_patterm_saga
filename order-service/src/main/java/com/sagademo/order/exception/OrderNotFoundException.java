package com.sagademo.order.exception;

/** The requested order does not exist in the Order database. */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
    }
}
