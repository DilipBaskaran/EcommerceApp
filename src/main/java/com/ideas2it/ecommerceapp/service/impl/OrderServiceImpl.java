package com.ideas2it.ecommerceapp.service.impl;

import com.ideas2it.ecommerceapp.exception.ProductUnavailableException;
import com.ideas2it.ecommerceapp.payment.strategy.PaymentDetails;
import com.ideas2it.ecommerceapp.payment.strategy.PaymentProcessingException;
import com.ideas2it.ecommerceapp.payment.strategy.PaymentStrategy;
import com.ideas2it.ecommerceapp.payment.strategy.PaymentStrategyFactory;
import jakarta.transaction.Transactional;
import com.ideas2it.ecommerceapp.model.Order;
import com.ideas2it.ecommerceapp.model.OrderItem;
import com.ideas2it.ecommerceapp.model.Product;
import com.ideas2it.ecommerceapp.model.User;
import com.ideas2it.ecommerceapp.repository.OrderRepository;
import com.ideas2it.ecommerceapp.repository.UserRepository;
import com.ideas2it.ecommerceapp.service.OrderService;
import com.ideas2it.ecommerceapp.service.ProductService;
import com.ideas2it.ecommerceapp.notification.observer.NotificationObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Implementation of the OrderService interface that provides functionality
 * for managing orders in the ecommerce application.
 * This service handles order operations like creating orders, processing payments,
 * updating order status, and retrieving order information.
 * It uses the Observer pattern for notifications and the Strategy pattern for payment processing.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final List<NotificationObserver> notificationObservers;
    private final PaymentStrategyFactory paymentStrategyFactory;

    @Autowired
    public OrderServiceImpl(
            OrderRepository orderRepository,
            UserRepository userRepository,
            ProductService productService,
            List<NotificationObserver> notificationObservers,
            PaymentStrategyFactory paymentStrategyFactory) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productService = productService;
        this.notificationObservers = notificationObservers;
        this.paymentStrategyFactory = paymentStrategyFactory;
    }

    /**
     * Retrieves all orders from the database.
     *
     * @return A list of all orders
     */
    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Retrieves a specific order by its ID.
     *
     * @param id The ID of the order to retrieve
     * @return The order with the specified ID
     * @throws NoSuchElementException If no order with the specified ID exists
     */
    @Override
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + id));
    }

    /**
     * Retrieves all orders for a specific user.
     *
     * @param userId The ID of the user whose orders are being retrieved
     * @return A list of orders belonging to the user
     * @throws NoSuchElementException If no user with the specified ID exists
     */
    @Override
    public List<Order> getOrdersByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));
        return orderRepository.findByUser(user);
    }

    /**
     * Retrieves a paginated list of orders for a specific user.
     *
     * @param userId The ID of the user whose orders are being retrieved
     * @param pageable Pagination information
     * @return A page of orders belonging to the user
     * @throws NoSuchElementException If no user with the specified ID exists
     */
    @Override
    public Page<Order> getOrdersByUserPaginated(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));
        return orderRepository.findByUser(user, pageable);
    }

    /**
     * Places a new order for a user with the specified items.
     * This method also updates product inventory.
     *
     * @param userId The ID of the user placing the order
     * @param items The set of items being ordered
     * @return The created order
     * @throws NoSuchElementException If no user with the specified ID exists
     */
    @Override
    @Transactional
    public Order placeOrder(Long userId, Set<OrderItem> items) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));

        // Create new order
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);

        // Add items to order and calculate total
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            Product product = productService.getProductById(item.getProduct().getId());

            // Update inventory
            productService.updateProductStock(product.getId(), item.getQuantity());

            // Set order item details
            item.setProduct(product);
            item.setUnitPrice(product.getPrice());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            order.addItem(item);

            // Add to total
            total = total.add(item.getSubtotal());
        }

        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    /**
     * Places a new order with payment processing.
     * This method creates an order, processes payment using the specified payment method,
     * updates product inventory, and notifies observers of successful orders.
     *
     * @param userId The ID of the user placing the order
     * @param items The set of items being ordered
     * @param paymentMethod The payment method to use (e.g., "CREDIT_CARD", "PAYPAL")
     * @param paymentDetails Payment details required for processing
     * @return The created order
     * @throws NoSuchElementException If no user with the specified ID exists
     * @throws RuntimeException If payment processing fails
     */
    @Override
    @Transactional
    public Order placeOrderWithPayment(Long userId, Set<OrderItem> items, String paymentMethod, PaymentDetails paymentDetails) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));

        // Create new order
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);

        // Add items to order and calculate total
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            Product product = productService.getProductById(item.getProduct().getId());

            if (product == null) {
                throw new ProductUnavailableException("Product not found with id: " + item.getProduct().getId());
            }
            // Update inventory
            productService.updateProductStock(product.getId(), item.getQuantity());

            // Set order item details
            item.setProduct(product);
            item.setUnitPrice(product.getPrice());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            order.addItem(item);

            // Add to total
            total = total.add(item.getSubtotal());
        }

        order.setTotalAmount(total);

        // Select payment strategy using factory
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(paymentMethod);

        // Process payment
        try {
            String transactionId = strategy.processPayment(total, paymentDetails);
            order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
            order.setStatus(Order.OrderStatus.PROCESSING);
            // Optionally store transactionId in order if needed
        } catch (PaymentProcessingException e) {
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            order.setStatus(Order.OrderStatus.PENDING);
            // Optionally log or handle payment failure
            throw new RuntimeException("Payment failed: " + e.getMessage(), e);
        }

        // Save order
        Order savedOrder = orderRepository.save(order);
        // Notify observers if payment succeeded
        if (order.getPaymentStatus() == Order.PaymentStatus.COMPLETED
                && notificationObservers != null && !notificationObservers.isEmpty()) {
            for (NotificationObserver observer : notificationObservers) {
                observer.notify(savedOrder, "Order placed and payment completed.");
            }
        }
        return savedOrder;
    }

    /**
     * Updates the status of an existing order.
     * If the order is canceled, product inventory is restored.
     *
     * @param orderId The ID of the order to update
     * @param status The new status for the order
     * @return The updated order
     * @throws NoSuchElementException If no order with the specified ID exists
     */
    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = getOrderById(orderId);
        order.setStatus(status);

        // If order is canceled, restore inventory
        if (status == Order.OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                // Product will be saved by cascade
            }
        }

        return orderRepository.save(order);
    }

    /**
     * Updates the payment status of an existing order.
     * If payment is completed and the order is pending, the order status is updated to processing.
     *
     * @param orderId The ID of the order to update
     * @param paymentStatus The new payment status
     * @return The updated order
     * @throws NoSuchElementException If no order with the specified ID exists
     */
    @Override
    @Transactional
    public Order updatePaymentStatus(Long orderId, Order.PaymentStatus paymentStatus) {
        Order order = getOrderById(orderId);
        order.setPaymentStatus(paymentStatus);

        // If payment is completed, update order status to processing
        if (paymentStatus == Order.PaymentStatus.COMPLETED &&
            order.getStatus() == Order.OrderStatus.PENDING) {
            order.setStatus(Order.OrderStatus.PROCESSING);
        }

        return orderRepository.save(order);
    }

    /**
     * Retrieves all orders with a specific status.
     *
     * @param status The status to filter orders by
     * @return A list of orders with the specified status
     */
    @Override
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    /**
     * Retrieves all orders placed within a specific date range.
     *
     * @param start The start of the date range
     * @param end The end of the date range
     * @return A list of orders placed within the date range
     */
    @Override
    public List<Order> getOrdersInDateRange(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findByOrderDateBetween(start, end);
    }

    /**
     * Counts the number of orders placed since a specific time.
     *
     * @param since The time threshold
     * @return The number of orders placed since the specified time
     */
    @Override
    public Long countRecentOrders(LocalDateTime since) {
        return orderRepository.countOrdersSince(since);
    }
}
