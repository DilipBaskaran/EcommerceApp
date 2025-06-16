package com.ideas2it.ecommerceapp.payment.strategy;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * A class to hold payment details for different payment methods.
 * This class is flexible and can accommodate various types of payment information.
 */
@Data
@Builder
public class PaymentDetails {
    // Common details
    private String customerId;
    private String customerEmail;
    private String customerName;

    // Credit Card specific details
    private String cardNumber;
    private String cardHolderName;
    private String expiryDate;
    private String cvv;

    // PayPal specific details
    private String paypalEmail;
    private String paypalToken;

    // Bank Transfer specific details
    private String bankAccountNumber;
    private String bankRoutingNumber;
    private String bankName;

    // Additional dynamic properties that might be needed for different payment methods
    private Map<String, String> additionalProperties;
}
