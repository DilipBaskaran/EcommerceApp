package com.ideas2it.ecommerceapp.controller;

import com.ideas2it.ecommerceapp.dto.ApiResponse;
import com.ideas2it.ecommerceapp.dto.OrderDto;
import com.ideas2it.ecommerceapp.dto.OrderPaymentRequest;
import com.ideas2it.ecommerceapp.model.Order;
import com.ideas2it.ecommerceapp.model.OrderItem;
import com.ideas2it.ecommerceapp.model.Product;
import com.ideas2it.ecommerceapp.model.User;
import com.ideas2it.ecommerceapp.payment.strategy.PaymentDetails;
import com.ideas2it.ecommerceapp.service.OrderService;
import com.ideas2it.ecommerceapp.service.ShoppingCartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderControllerTest {

    @InjectMocks
    private OrderController orderController;

    @Mock
    private OrderService orderService;

    @Mock
    private ShoppingCartService shoppingCartService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private User testUser;
    private Product testProduct;
    private Order testOrder;
    private OrderItem testOrderItem;
    private Set<OrderItem> orderItems;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup security context mock
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");

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
    void testGetAllOrders_ReturnsOrders() {
        // Arrange
        List<Order> orders = Arrays.asList(testOrder, new Order());
        when(orderService.getAllOrders()).thenReturn(orders);

        // Act
        ResponseEntity<ApiResponse<List<Order>>> response = orderController.getAllOrders();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Orders retrieved successfully", response.getBody().getMessage());
        assertEquals(2, response.getBody().getData().size());
    }

    @Test
    void testGetAllOrders_ServiceThrowsException_ReturnsErrorResponse() {
        // Arrange
        when(orderService.getAllOrders()).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<ApiResponse<List<Order>>> response = orderController.getAllOrders();

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Database error", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testGetOrderById_ExistingOrder_ReturnsOrder() {
        // Arrange
        when(orderService.getOrderById(1L)).thenReturn(testOrder);

        // Act
        ResponseEntity<ApiResponse<Order>> response = orderController.getOrderById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Order retrieved successfully", response.getBody().getMessage());
        assertEquals(1L, response.getBody().getData().getId());
    }

    @Test
    void testGetOrderById_NonExistingOrder_ReturnsErrorResponse() {
        // Arrange
        when(orderService.getOrderById(99L)).thenThrow(new NoSuchElementException("Order not found with id: 99"));

        // Act
        ResponseEntity<ApiResponse<Order>> response = orderController.getOrderById(99L);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Order not found with id: 99", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testGetMyOrders_ReturnsUserOrders() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Order> userOrders = Arrays.asList(testOrder);
        Page<Order> orderPage = new PageImpl<>(userOrders, pageable, 1);

        when(authentication.getName()).thenReturn("testuser");
        when(orderService.getOrdersByUserPaginated(anyLong(), eq(pageable))).thenReturn(orderPage);

        // Act
        ResponseEntity<ApiResponse<Page<Order>>> response = orderController.getMyOrders(pageable, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("User orders retrieved successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().getTotalElements());
    }

    @Test
    void testGetMyOrdersPaginated_ReturnsPaginatedUserOrders() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Order> userOrders = Arrays.asList(testOrder);
        Page<Order> orderPage = new PageImpl<>(userOrders, pageable, 1);

        when(authentication.getName()).thenReturn("testuser");
        when(orderService.getOrdersByUserPaginated(anyLong(), eq(pageable))).thenReturn(orderPage);

        // Act
        ResponseEntity<ApiResponse<Page<Order>>> response = orderController.getMyOrders(pageable, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("User orders retrieved successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().getTotalElements());
    }

    @Test
    void testPlaceOrder_CreatesOrder() {
        // Arrange
        when(authentication.getName()).thenReturn("testuser");
        when(orderService.placeOrder(anyLong(), eq(orderItems))).thenReturn(testOrder);

        // Act
        ResponseEntity<ApiResponse<Order>> response = orderController.placeOrder(orderItems, authentication);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Order placed successfully", response.getBody().getMessage());
        assertEquals(1L, response.getBody().getData().getId());
    }

    @Test
    void testPlaceOrderWithPayment_ProcessesPaymentAndCreatesOrder() {
        // Arrange
        OrderPaymentRequest paymentRequest = new OrderPaymentRequest();
        paymentRequest.setPaymentMethod("CREDIT_CARD");

        // Create PaymentDetails using builder pattern
        PaymentDetails paymentDetails = PaymentDetails.builder()
            .cardNumber("4111111111111111")
            .expiryDate("12/25")
            .cvv("123")
            .build();

        // Set PaymentDetails object on the request
        paymentRequest.setPaymentDetails(paymentDetails);
        paymentRequest.setItems(orderItems);

        when(authentication.getName()).thenReturn("testuser");
        when(orderService.placeOrderWithPayment(
                anyLong(),
                eq(orderItems),
                eq("CREDIT_CARD"),
                eq(paymentDetails)
            )).thenReturn(testOrder);

        // Act
        ResponseEntity<ApiResponse<Order>> response = orderController.placeOrderWithPayment(paymentRequest, authentication);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Order placed and payment processed successfully", response.getBody().getMessage());
        assertEquals(1L, response.getBody().getData().getId());
    }

    @Test
    void testPlaceOrderWithPayment_PaymentFails_ReturnsErrorResponse() {
        // Arrange
        OrderPaymentRequest paymentRequest = new OrderPaymentRequest();
        paymentRequest.setPaymentMethod("CREDIT_CARD");

        // Create PaymentDetails using builder pattern
        PaymentDetails paymentDetails = PaymentDetails.builder()
            .cardNumber("4111111111111111")
            .expiryDate("12/25")
            .cvv("123")
            .build();

        // Set PaymentDetails object on the request
        paymentRequest.setPaymentDetails(paymentDetails);
        paymentRequest.setItems(orderItems);

        when(authentication.getName()).thenReturn("testuser");
        when(orderService.placeOrderWithPayment(
                anyLong(),
                eq(orderItems),
                eq("CREDIT_CARD"),
                eq(paymentDetails)
            )).thenThrow(new RuntimeException("Payment failed: Invalid credit card"));

        // Act
        ResponseEntity<ApiResponse<Order>> response = orderController.placeOrderWithPayment(paymentRequest, authentication);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Payment failed: Invalid credit card", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testUpdateOrderStatus_UpdatesStatus() {
        // Arrange
        when(orderService.updateOrderStatus(1L, Order.OrderStatus.SHIPPED)).thenReturn(testOrder);

        // Act
        ResponseEntity<ApiResponse<Order>> response = orderController.updateOrderStatus(1L, "SHIPPED");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Order status updated successfully", response.getBody().getMessage());
        assertEquals(1L, response.getBody().getData().getId());
    }

    @Test
    void testUpdateOrderStatus_InvalidStatus_ReturnsErrorResponse() {
        // Arrange
        // No need to mock service as it shouldn't be called

        // Act
        ResponseEntity<ApiResponse<Order>> response = orderController.updateOrderStatus(1L, "INVALID_STATUS");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Invalid order status"));
        assertNull(response.getBody().getData());
    }

    @Test
    void testUpdatePaymentStatus_UpdatesPaymentStatus() {
        // Arrange
        when(orderService.updatePaymentStatus(1L, Order.PaymentStatus.COMPLETED)).thenReturn(testOrder);

        // Act
        ResponseEntity<ApiResponse<Order>> response = orderController.updatePaymentStatus(1L, "COMPLETED");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Payment status updated successfully", response.getBody().getMessage());
        assertEquals(1L, response.getBody().getData().getId());
    }

    @Test
    void testUpdatePaymentStatus_InvalidStatus_ReturnsErrorResponse() {
        // Arrange
        // No need to mock service as it shouldn't be called

        // Act
        ResponseEntity<ApiResponse<Order>> response = orderController.updatePaymentStatus(1L, "INVALID_STATUS");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Invalid payment status"));
        assertNull(response.getBody().getData());
    }

    @Test
    void testGetOrdersByStatus_ReturnsFilteredOrders() {
        // Arrange
        List<Order> pendingOrders = Arrays.asList(testOrder);
        when(orderService.getOrdersByStatus(Order.OrderStatus.PENDING)).thenReturn(pendingOrders);

        // Act
        ResponseEntity<ApiResponse<List<Order>>> response = orderController.getOrdersByStatus("PENDING");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Orders retrieved successfully", response.getBody().getMessage());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void testGetOrdersByStatus_InvalidStatus_ReturnsErrorResponse() {
        // Arrange
        // No need to mock service as it shouldn't be called

        // Act
        ResponseEntity<ApiResponse<List<Order>>> response = orderController.getOrdersByStatus("INVALID_STATUS");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Invalid order status"));
        assertNull(response.getBody().getData());
    }
}
