package com.ideas2it.ecommerceapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import com.ideas2it.ecommerceapp.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record OrderDto(
    Long id,

    @NotNull(message = "User ID is required")
    Long userId,

    @NotNull(message = "Order date is required")
    @PastOrPresent(message = "Order date cannot be in the future")
    LocalDateTime orderDate,

    @NotNull(message = "Order status is required")
    Order.OrderStatus status,

    @NotNull(message = "Total amount is required")
    @PositiveOrZero(message = "Total amount must be greater than or equal to 0")
    BigDecimal totalAmount,

    @NotNull(message = "Payment status is required")
    Order.PaymentStatus paymentStatus,

    String paymentMethod,

    String shippingAddress,

    Set<OrderItemDto> items
) {
    // Compact canonical constructor for validation
    public OrderDto {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (orderDate == null) {
            throw new IllegalArgumentException("Order date cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Order status cannot be null");
        }
        if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total amount cannot be negative");
        }
        if (paymentStatus == null) {
            throw new IllegalArgumentException("Payment status cannot be null");
        }
    }
}
