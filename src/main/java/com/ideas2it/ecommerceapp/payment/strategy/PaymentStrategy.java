package com.ideas2it.ecommerceapp.payment.strategy;

import java.math.BigDecimal;

/**
 * Payment strategy interface that defines the contract for different payment methods.
 * Each payment method will implement this interface with its own processing logic.
 */
public interface PaymentStrategy {
    /**
     * Process a payment with the given payment details.
     *
     * @param amount The amount to be processed
     * @param paymentDetails Additional details needed for the payment method
     * @return A transaction ID or confirmation code for the payment
     * @throws PaymentProcessingException if payment processing fails
     */
    String processPayment(BigDecimal amount, PaymentDetails paymentDetails) throws PaymentProcessingException;

    /**
     * Refund a previously made payment.
     *
     * @param transactionId The ID of the transaction to refund
     * @param amount The amount to refund (may be partial)
     * @param paymentDetails Additional details needed for the refund
     * @return A confirmation code for the refund
     * @throws PaymentProcessingException if refund processing fails
     */
    String refundPayment(String transactionId, BigDecimal amount, PaymentDetails paymentDetails) throws PaymentProcessingException;

    /**
     * Validates if the payment details are sufficient for this payment method.
     *
     * @param paymentDetails The payment details to validate
     * @return true if the details are valid, false otherwise
     */
    boolean validatePaymentDetails(PaymentDetails paymentDetails);

    /**
     * Returns the name or identifier of this payment method.
     *
     * @return The payment method name (e.g., "Credit Card", "PayPal")
     */
    String getPaymentMethodName();
}
