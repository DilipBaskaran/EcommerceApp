package com.ideas2it.ecommerceapp.notification.observer;

import com.ideas2it.ecommerceapp.model.Order.OrderStatus;
import lombok.Getter;

/**
 * Event that occurs when an order status changes.
 */
@Getter
public class OrderStatusChangedEvent extends BaseEvent {
    private final String orderId;
    private final OrderStatus oldStatus;
    private final OrderStatus newStatus;
    private final String customerEmail;

    public OrderStatusChangedEvent(String orderId, OrderStatus oldStatus,
                                 OrderStatus newStatus, String customerEmail) {
        super("ORDER_STATUS_CHANGED");
        this.orderId = orderId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.customerEmail = customerEmail;
    }

    @Override
    public String getDescription() {
        return String.format("Order %s status changed from %s to %s",
                orderId, oldStatus, newStatus);
    }
}
