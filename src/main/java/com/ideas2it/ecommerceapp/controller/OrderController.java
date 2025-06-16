package com.ideas2it.ecommerceapp.controller;

import jakarta.validation.Valid;
import com.ideas2it.ecommerceapp.dto.ApiResponse;
import com.ideas2it.ecommerceapp.dto.OrderPaymentRequest;
import com.ideas2it.ecommerceapp.exception.GlobalExceptionHandler;
import com.ideas2it.ecommerceapp.model.Order;
import com.ideas2it.ecommerceapp.model.OrderItem;
import com.ideas2it.ecommerceapp.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Order>>> getAllOrders() {
        try {
            List<Order> orders = orderService.getAllOrders();
            return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getOrderById(@PathVariable Long id) {
        try {
            Order order = orderService.getOrderById(id);

            // Check if the user has access to this order
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) &&
                    !order.getUser().getUsername().equals(auth.getName())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You don't have permission to view this order", null));
            }

            return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<Page<Order>>> getMyOrders(Pageable pageable, Authentication authentication) {
        try {
            String username = authentication.getName();
            // Get user ID from username would be handled in service
            Long userId = 0L; // This would be retrieved from userService
            Page<Order> orders = orderService.getOrdersByUserPaginated(userId, pageable);
            return ResponseEntity.ok(ApiResponse.success("User orders retrieved successfully", orders));
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Order>> placeOrder(@Valid @RequestBody Set<OrderItem> items, Authentication authentication) {
        try {
            String username = authentication.getName();
            // Get user ID from username would be handled in service
            Long userId = 0L; // This would be retrieved from userService
            Order order = orderService.placeOrder(userId, items);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Order placed successfully", order));
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/with-payment")
    public ResponseEntity<ApiResponse<Order>> placeOrderWithPayment(@Valid @RequestBody OrderPaymentRequest request, Authentication authentication) {
        try {
            String username = authentication.getName();
            // Get user ID from username would be handled in service
            Long userId = 0L; // This would be retrieved from userService
            Order order = orderService.placeOrderWithPayment(userId, request.getItems(), request.getPaymentMethod(), request.getPaymentDetails());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Order placed and payment processed successfully", order));
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Order>> updateOrderStatus(@PathVariable Long id,
                                               @RequestParam String status) {
        try {
            Order.OrderStatus orderStatus;
            try {
                orderStatus = Order.OrderStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid order status: " + status, null));
            }

            Order order = orderService.updateOrderStatus(id, orderStatus);
            return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", order));
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}/payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Order>> updatePaymentStatus(@PathVariable Long id,
                                                @RequestParam String paymentStatus) {
        try {
            Order.PaymentStatus status;
            try {
                status = Order.PaymentStatus.valueOf(paymentStatus);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid payment status: " + paymentStatus, null));
            }

            Order order = orderService.updatePaymentStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Payment status updated successfully", order));
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/by-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Order>>> getOrdersByStatus(@RequestParam String status) {
        try {
            Order.OrderStatus orderStatus;
            try {
                orderStatus = Order.OrderStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid order status: " + status, null));
            }

            List<Order> orders = orderService.getOrdersByStatus(orderStatus);
            return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/by-date")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Order>>> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        try {
            List<Order> orders = orderService.getOrdersInDateRange(start, end);
            return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> countRecentOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        try {
            Long count = orderService.countRecentOrders(since);
            return ResponseEntity.ok(ApiResponse.success("Order count retrieved successfully", count));
        } catch (Exception e) {
            return GlobalExceptionHandler.errorResponseEntity(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
