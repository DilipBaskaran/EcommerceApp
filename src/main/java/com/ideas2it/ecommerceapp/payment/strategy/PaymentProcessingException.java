package com.ideas2it.ecommerceapp.payment.strategy;

/**
 * Exception thrown when payment processing fails.
 */
public class PaymentProcessingException extends Exception {

    public PaymentProcessingException(String message) {
        super(message);
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
