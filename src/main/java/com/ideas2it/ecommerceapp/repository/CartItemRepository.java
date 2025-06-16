package com.ideas2it.ecommerceapp.repository;

import com.ideas2it.ecommerceapp.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
