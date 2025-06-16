package com.ideas2it.ecommerceapp.service;

import com.ideas2it.ecommerceapp.model.Order;
import com.ideas2it.ecommerceapp.model.OrderItem;
import com.ideas2it.ecommerceapp.payment.strategy.PaymentDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface OrderService {
    List<Order> getAllOrders();

    Order getOrderById(Long id);

    List<Order> getOrdersByUser(Long userId);

    Page<Order> getOrdersByUserPaginated(Long userId, Pageable pageable);

    Order placeOrder(Long userId, Set<OrderItem> items);

    Order updateOrderStatus(Long orderId, Order.OrderStatus status);

    Order updatePaymentStatus(Long orderId, Order.PaymentStatus paymentStatus);

    List<Order> getOrdersByStatus(Order.OrderStatus status);

    List<Order> getOrdersInDateRange(LocalDateTime start, LocalDateTime end);

    Long countRecentOrders(LocalDateTime since);

    /**
     * Place an order with payment processing.
     * @param userId the user placing the order
     * @param items the items to order
     * @param paymentMethod the payment method (e.g., "CreditCard", "Paypal", "BankTransfer")
     * @param paymentDetails the payment details
     * @return the placed Order
     */
    Order placeOrderWithPayment(Long userId, Set<OrderItem> items, String paymentMethod, PaymentDetails paymentDetails);
}
