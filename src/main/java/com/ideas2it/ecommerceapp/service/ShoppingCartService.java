package com.ideas2it.ecommerceapp.service;

import com.ideas2it.ecommerceapp.model.Cart;

public interface ShoppingCartService {
    void addProductToCart(Long userId, Long productId, int quantity);
    Cart getCartForUser(Long userId);
    void updateProductQuantity(Long userId, Long productId, int quantity);
    void removeProductFromCart(Long userId, Long productId);
    void clearCart(Long userId);
}

