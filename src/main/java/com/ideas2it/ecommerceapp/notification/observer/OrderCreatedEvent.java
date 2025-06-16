package com.ideas2it.ecommerceapp.notification.observer;

import com.ideas2it.ecommerceapp.model.Order;
import lombok.Getter;

/**
 * Event that occurs when an order is created.
 */
@Getter
public class OrderCreatedEvent extends BaseEvent {
    private final Order order;
    private final String customerEmail;
    private final String customerName;

    public OrderCreatedEvent(Order order, String customerEmail, String customerName) {
        super("ORDER_CREATED");
        this.order = order;
        this.customerEmail = customerEmail;
        this.customerName = customerName;
    }

    @Override
    public String getDescription() {
        return String.format("Order %s created by %s at %s",
                order.getId(), customerName, getTimestamp());
    }
}
