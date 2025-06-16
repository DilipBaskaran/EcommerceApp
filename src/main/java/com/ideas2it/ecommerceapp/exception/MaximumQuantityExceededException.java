package com.ideas2it.ecommerceapp.exception;

/**
 * Exception thrown when a user attempts to add more than the maximum allowed quantity
 * of a specific product to their shopping cart.
 */
public class MaximumQuantityExceededException extends RuntimeException {

    public MaximumQuantityExceededException(String message) {
        super(message);
    }

    public MaximumQuantityExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
