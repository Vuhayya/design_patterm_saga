package com.sagademo.inventory.exception;

/** Unknown product id or unknown reservation. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
