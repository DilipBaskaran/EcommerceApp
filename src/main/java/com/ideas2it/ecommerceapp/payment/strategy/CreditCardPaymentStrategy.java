package com.ideas2it.ecommerceapp.payment.strategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * Credit Card payment strategy implementation.
 */
@Component
@Slf4j
public class CreditCardPaymentStrategy implements PaymentStrategy {

    @Override
    public String processPayment(BigDecimal amount, PaymentDetails paymentDetails) throws PaymentProcessingException {
        if (!validatePaymentDetails(paymentDetails)) {
            throw new PaymentProcessingException("Invalid credit card payment details");
        }

        try {
            // In a real implementation, this would integrate with a payment gateway
            log.info("Processing credit card payment of {} for {}", amount, paymentDetails.getCustomerName());

            // Simulate payment processing
            String cardNumberMasked = maskCardNumber(paymentDetails.getCardNumber());
            log.info("Processing payment with card: {}", cardNumberMasked);

            // Generate a transaction ID (in real implementation, this would come from the payment gateway)
            String transactionId = "CC-" + UUID.randomUUID().toString();
            log.info("Credit card payment successful. Transaction ID: {}", transactionId);

            return transactionId;
        } catch (Exception e) {
            log.error("Credit card payment processing failed", e);
            throw new PaymentProcessingException("Credit card payment processing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String refundPayment(String transactionId, BigDecimal amount, PaymentDetails paymentDetails) throws PaymentProcessingException {
        try {
            // In a real implementation, this would integrate with a payment gateway
            log.info("Processing refund of {} for transaction: {}", amount, transactionId);

            // Generate a refund ID
            String refundId = "REF-" + UUID.randomUUID().toString();
            log.info("Credit card refund successful. Refund ID: {}", refundId);

            return refundId;
        } catch (Exception e) {
            log.error("Credit card refund processing failed", e);
            throw new PaymentProcessingException("Credit card refund processing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validatePaymentDetails(PaymentDetails paymentDetails) {
        // Basic validation - in a real implementation, this would be more comprehensive
        if (paymentDetails == null) {
            return false;
        }

        if (paymentDetails.getCardNumber() == null || paymentDetails.getCardNumber().trim().isEmpty()) {
            return false;
        }

        if (paymentDetails.getExpiryDate() == null || paymentDetails.getExpiryDate().trim().isEmpty()) {
            return false;
        }

        if (paymentDetails.getCvv() == null || paymentDetails.getCvv().trim().isEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    public String getPaymentMethodName() {
        return "Credit Card";
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }

        // Show only the last 4 digits
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }
}
