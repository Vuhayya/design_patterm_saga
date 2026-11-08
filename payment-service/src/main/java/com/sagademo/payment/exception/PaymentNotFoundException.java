package com.sagademo.payment.exception;

/** No payment exists for the given paymentId / sagaId. */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
