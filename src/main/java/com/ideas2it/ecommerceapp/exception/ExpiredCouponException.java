package com.ideas2it.ecommerceapp.exception;

public class ExpiredCouponException extends RuntimeException {
    public ExpiredCouponException(String message) {
        super(message);
    }
}
