package com.ideas2it.ecommerceapp.payment.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Payment service that uses the Strategy Pattern to process payments
 * through different payment methods.
 */
@Service
@Slf4j
public class PaymentService {

    private final Map<String, PaymentStrategy> paymentStrategies = new HashMap<>();

    @Autowired
    public PaymentService(List<PaymentStrategy> strategyList) {
        // Register all payment strategies by their method name
        for (PaymentStrategy strategy : strategyList) {
            paymentStrategies.put(strategy.getPaymentMethodName().toLowerCase(), strategy);
        }
        log.info("Registered payment strategies: {}", paymentStrategies.keySet());
    }

    /**
     * Process a payment using the specified payment method.
     *
     * @param paymentMethod The payment method to use (e.g., "credit card", "paypal")
     * @param amount The amount to charge
     * @param paymentDetails The payment details
     * @return A transaction ID or confirmation code
     * @throws PaymentProcessingException if payment processing fails or the payment method is not supported
     */
    public String processPayment(String paymentMethod, BigDecimal amount, PaymentDetails paymentDetails)
            throws PaymentProcessingException {
        PaymentStrategy strategy = getPaymentStrategy(paymentMethod);
        return strategy.processPayment(amount, paymentDetails);
    }

    /**
     * Refund a payment using the specified payment method.
     *
     * @param paymentMethod The payment method to use (e.g., "credit card", "paypal")
     * @param transactionId The original transaction ID
     * @param amount The amount to refund
     * @param paymentDetails The payment details
     * @return A refund confirmation code
     * @throws PaymentProcessingException if refund processing fails or the payment method is not supported
     */
    public String refundPayment(String paymentMethod, String transactionId, BigDecimal amount, PaymentDetails paymentDetails)
            throws PaymentProcessingException {
        PaymentStrategy strategy = getPaymentStrategy(paymentMethod);
        return strategy.refundPayment(transactionId, amount, paymentDetails);
    }

    /**
     * Validates payment details for the specified payment method.
     *
     * @param paymentMethod The payment method to use
     * @param paymentDetails The payment details to validate
     * @return true if the details are valid, false otherwise
     * @throws PaymentProcessingException if the payment method is not supported
     */
    public boolean validatePaymentDetails(String paymentMethod, PaymentDetails paymentDetails)
            throws PaymentProcessingException {
        PaymentStrategy strategy = getPaymentStrategy(paymentMethod);
        return strategy.validatePaymentDetails(paymentDetails);
    }

    /**
     * Gets the available payment methods.
     *
     * @return A list of available payment method names
     */
    public List<String> getAvailablePaymentMethods() {
        return paymentStrategies.keySet().stream().toList();
    }

    private PaymentStrategy getPaymentStrategy(String paymentMethod) throws PaymentProcessingException {
        PaymentStrategy strategy = paymentStrategies.get(paymentMethod.toLowerCase());
        if (strategy == null) {
            throw new PaymentProcessingException("Unsupported payment method: " + paymentMethod);
        }
        return strategy;
    }
}
