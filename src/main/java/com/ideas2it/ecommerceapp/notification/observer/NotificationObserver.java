package com.ideas2it.ecommerceapp.notification.observer;

import com.ideas2it.ecommerceapp.model.Order;

public interface NotificationObserver {
    void notify(Order order, String message);
}

