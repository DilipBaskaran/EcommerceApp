package com.ideas2it.ecommerceapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record OrderItemDto(
    Long id,

    @NotNull(message = "Product ID is required")
    Long productId,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity,

    @NotNull(message = "Unit price is required")
    @PositiveOrZero(message = "Unit price must be greater than or equal to 0")
    BigDecimal unitPrice,

    @NotNull(message = "Subtotal is required")
    @PositiveOrZero(message = "Subtotal must be greater than or equal to 0")
    BigDecimal subtotal,

    String productName
) {
    // Compact canonical constructor for validation
    public OrderItemDto {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (quantity != null && quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        if (subtotal != null && subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Subtotal cannot be negative");
        }
    }
}
