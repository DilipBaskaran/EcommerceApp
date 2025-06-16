package com.ideas2it.ecommerceapp.notification.observer;

import com.ideas2it.ecommerceapp.model.Order;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationObserver implements NotificationObserver {
    @Override
    public void notify(Order order, String message) {
        // Simulate sending an email notification
        System.out.println("Email sent to: " + order.getUser().getEmail() + " | Message: " + message);
    }
}

