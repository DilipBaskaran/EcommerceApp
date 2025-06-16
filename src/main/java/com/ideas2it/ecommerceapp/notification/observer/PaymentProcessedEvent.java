package com.ideas2it.ecommerceapp.notification.observer;

import java.math.BigDecimal;
import lombok.Getter;

/**
 * Event that occurs when a payment is processed.
 */
@Getter
public class PaymentProcessedEvent extends BaseEvent {
    private final String orderId;
    private final BigDecimal amount;
    private final String paymentMethod;
    private final String transactionId;
    private final String customerEmail;

    public PaymentProcessedEvent(String orderId, BigDecimal amount, String paymentMethod,
                                String transactionId, String customerEmail) {
        super("PAYMENT_PROCESSED");
        this.orderId = orderId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.customerEmail = customerEmail;
    }

    @Override
    public String getDescription() {
        return String.format("Payment of %s processed for order %s using %s. Transaction ID: %s",
                amount, orderId, paymentMethod, transactionId);
    }
}
