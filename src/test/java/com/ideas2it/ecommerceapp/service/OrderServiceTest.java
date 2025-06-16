package com.ideas2it.ecommerceapp.service;

import com.ideas2it.ecommerceapp.model.Order;
import com.ideas2it.ecommerceapp.model.OrderItem;
import com.ideas2it.ecommerceapp.model.Product;
import com.ideas2it.ecommerceapp.model.User;
import com.ideas2it.ecommerceapp.notification.observer.NotificationObserver;
import com.ideas2it.ecommerceapp.payment.strategy.PaymentDetails;
import com.ideas2it.ecommerceapp.payment.strategy.PaymentProcessingException;
import com.ideas2it.ecommerceapp.payment.strategy.PaymentStrategy;
import com.ideas2it.ecommerceapp.payment.strategy.PaymentStrategyFactory;
import com.ideas2it.ecommerceapp.repository.OrderRepository;
import com.ideas2it.ecommerceapp.repository.UserRepository;
import com.ideas2it.ecommerceapp.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductService productService;

    @Mock
    private List<NotificationObserver> notificationObservers;

    @Mock
    private PaymentStrategyFactory paymentStrategyFactory;

    @Mock
    private PaymentStrategy paymentStrategy;

    @Mock
    private NotificationObserver notificationObserver;

    private User testUser;
    private Product testProduct;
    private Order testOrder;
    private OrderItem testOrderItem;
    private Set<OrderItem> orderItems;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize the list of notification observers
        when(notificationObservers.get(0)).thenReturn(notificationObserver);

        // Create OrderService with constructor injection
        orderService = new OrderServiceImpl(
            orderRepository,
            userRepository,
            productService,
            notificationObservers,
            paymentStrategyFactory
        );

        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // Create test product
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(BigDecimal.valueOf(10.0));
        testProduct.setStockQuantity(100);

        // Create test order item
        testOrderItem = new OrderItem();
        testOrderItem.setId(1L);
        testOrderItem.setProduct(testProduct);
        testOrderItem.setQuantity(2);
        testOrderItem.setUnitPrice(testProduct.getPrice());
        testOrderItem.setSubtotal(testProduct.getPrice().multiply(BigDecimal.valueOf(testOrderItem.getQuantity())));

        // Create set of order items
        orderItems = new HashSet<>();
        orderItems.add(testOrderItem);

        // Create test order
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder.setStatus(Order.OrderStatus.PENDING);
        testOrder.setPaymentStatus(Order.PaymentStatus.PENDING);
        testOrder.setTotalAmount(BigDecimal.valueOf(20.0));
        testOrder.setItems(orderItems);
    }

    @Test
    void testGetAllOrders_ReturnsAllOrders() {
        // Arrange
        List<Order> orders = Arrays.asList(testOrder, new Order());
        when(orderRepository.findAll()).thenReturn(orders);

        // Act
        List<Order> result = orderService.getAllOrders();

        // Assert
        assertEquals(2, result.size());
        verify(orderRepository).findAll();
    }

    @Test
    void testGetOrderById_ExistingOrder_ReturnsOrder() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act
        Order result = orderService.getOrderById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(orderRepository).findById(1L);
    }

    @Test
    void testGetOrderById_NonExistingOrder_ThrowsException() {
        // Arrange
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            orderService.getOrderById(99L);
        });
        verify(orderRepository).findById(99L);
    }

    @Test
    void testGetOrdersByUser_ReturnsUserOrders() {
        // Arrange
        List<Order> userOrders = Arrays.asList(testOrder);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUser(testUser)).thenReturn(userOrders);

        // Act
        List<Order> result = orderService.getOrdersByUser(1L);

        // Assert
        assertEquals(1, result.size());
        verify(userRepository).findById(1L);
        verify(orderRepository).findByUser(testUser);
    }

    @Test
    void testGetOrdersByUserPaginated_ReturnsPaginatedUserOrders() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Order> userOrders = Arrays.asList(testOrder);
        Page<Order> orderPage = new PageImpl<>(userOrders, pageable, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUser(testUser, pageable)).thenReturn(orderPage);

        // Act
        Page<Order> result = orderService.getOrdersByUserPaginated(1L, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findById(1L);
        verify(orderRepository).findByUser(testUser, pageable);
    }

    @Test
    void testPlaceOrder_CreatesAndReturnsOrder() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productService.getProductById(1L)).thenReturn(testProduct);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            savedOrder.setId(1L);
            return savedOrder;
        });

        // Act
        Order result = orderService.placeOrder(1L, orderItems);

        // Assert
        assertNotNull(result);
        assertEquals(Order.OrderStatus.PENDING, result.getStatus());
        assertEquals(Order.PaymentStatus.PENDING, result.getPaymentStatus());
        assertEquals(testUser, result.getUser());
        verify(productService).updateProductStock(1L, 2);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testPlaceOrderWithPayment_SuccessfulPayment_CreatesOrderWithCompletedPayment() throws PaymentProcessingException {
        // Arrange
        PaymentDetails paymentDetails = PaymentDetails.builder()
            .cardNumber("4111111111111111")
            .expiryDate("12/25")
            .cvv("123")
            .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productService.getProductById(1L)).thenReturn(testProduct);
        when(paymentStrategyFactory.getStrategy("CREDIT_CARD")).thenReturn(paymentStrategy);
        when(paymentStrategy.processPayment(any(BigDecimal.class), any(PaymentDetails.class))).thenReturn("TX123456");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            savedOrder.setId(1L);
            return savedOrder;
        });

        // Act
        Order result = orderService.placeOrderWithPayment(1L, orderItems, "CREDIT_CARD", paymentDetails);

        // Assert
        assertNotNull(result);
        assertEquals(Order.OrderStatus.PROCESSING, result.getStatus());
        assertEquals(Order.PaymentStatus.COMPLETED, result.getPaymentStatus());
        verify(paymentStrategy).processPayment(any(BigDecimal.class), eq(paymentDetails));
        verify(orderRepository).save(any(Order.class));
        verify(notificationObservers.get(0)).notify(result, "Order placed and payment completed.");
    }

    @Test
    void testPlaceOrderWithPayment_FailedPayment_ThrowsException() throws PaymentProcessingException {
        // Arrange
        PaymentDetails paymentDetails = PaymentDetails.builder()
            .cardNumber("4111111111111111")
            .expiryDate("12/25")
            .cvv("123")
            .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productService.getProductById(1L)).thenReturn(testProduct);
        when(paymentStrategyFactory.getStrategy("CREDIT_CARD")).thenReturn(paymentStrategy);
        when(paymentStrategy.processPayment(any(BigDecimal.class), any(PaymentDetails.class)))
            .thenThrow(new PaymentProcessingException("Payment declined"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            orderService.placeOrderWithPayment(1L, orderItems, "CREDIT_CARD", paymentDetails);
        });
    }

    @Test
    void testUpdateOrderStatus_UpdatesStatus() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        Order result = orderService.updateOrderStatus(1L, Order.OrderStatus.SHIPPED);

        // Assert
        assertEquals(Order.OrderStatus.SHIPPED, result.getStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    void testUpdateOrderStatus_CancelledOrder_RestoresInventory() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        Order result = orderService.updateOrderStatus(1L, Order.OrderStatus.CANCELLED);

        // Assert
        assertEquals(Order.OrderStatus.CANCELLED, result.getStatus());
        assertEquals(102, testProduct.getStockQuantity()); // Original 100 + returned 2
        verify(orderRepository).save(testOrder);
    }

    @Test
    void testUpdatePaymentStatus_UpdatesPaymentStatus() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        Order result = orderService.updatePaymentStatus(1L, Order.PaymentStatus.COMPLETED);

        // Assert
        assertEquals(Order.PaymentStatus.COMPLETED, result.getPaymentStatus());
        assertEquals(Order.OrderStatus.PROCESSING, result.getStatus()); // Should update order status too
        verify(orderRepository).save(testOrder);
    }

    @Test
    void testGetOrdersByStatus_ReturnsOrdersWithStatus() {
        // Arrange
        List<Order> pendingOrders = Arrays.asList(testOrder);
        when(orderRepository.findByStatus(Order.OrderStatus.PENDING)).thenReturn(pendingOrders);

        // Act
        List<Order> result = orderService.getOrdersByStatus(Order.OrderStatus.PENDING);

        // Assert
        assertEquals(1, result.size());
        verify(orderRepository).findByStatus(Order.OrderStatus.PENDING);
    }

    @Test
    void testGetOrdersInDateRange_ReturnsOrdersInRange() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        List<Order> recentOrders = Arrays.asList(testOrder);

        when(orderRepository.findByOrderDateBetween(start, end)).thenReturn(recentOrders);

        // Act
        List<Order> result = orderService.getOrdersInDateRange(start, end);

        // Assert
        assertEquals(1, result.size());
        verify(orderRepository).findByOrderDateBetween(start, end);
    }

    @Test
    void testCountRecentOrders_ReturnsCount() {
        // Arrange
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        when(orderRepository.countOrdersSince(since)).thenReturn(5L);

        // Act
        Long result = orderService.countRecentOrders(since);

        // Assert
        assertEquals(5L, result);
        verify(orderRepository).countOrdersSince(since);
    }
}
