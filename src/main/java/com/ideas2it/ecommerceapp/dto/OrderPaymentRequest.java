package com.ideas2it.ecommerceapp.dto;

import com.ideas2it.ecommerceapp.model.OrderItem;
import com.ideas2it.ecommerceapp.payment.strategy.PaymentDetails;
import lombok.Data;
import java.util.Set;

@Data
public class OrderPaymentRequest {
    private Set<OrderItem> items;
    private String paymentMethod;
    private PaymentDetails paymentDetails;
}

