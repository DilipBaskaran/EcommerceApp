package com.ideas2it.ecommerceapp.service.impl;

import com.ideas2it.ecommerceapp.exception.EmptyCartException;
import com.ideas2it.ecommerceapp.exception.InvalidCartException;
import com.ideas2it.ecommerceapp.exception.OutOfStockException;
import com.ideas2it.ecommerceapp.exception.ProductUnavailableException;
import com.ideas2it.ecommerceapp.exception.MaximumQuantityExceededException;
import com.ideas2it.ecommerceapp.model.Cart;
import com.ideas2it.ecommerceapp.model.CartItem;
import com.ideas2it.ecommerceapp.model.Product;
import com.ideas2it.ecommerceapp.service.ProductService;
import com.ideas2it.ecommerceapp.service.ShoppingCartService;
import com.ideas2it.ecommerceapp.repository.CartRepository;
import com.ideas2it.ecommerceapp.repository.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of the ShoppingCartService interface that provides functionality
 * for managing user shopping carts in the ecommerce application.
 * This service handles cart operations like adding products, updating quantities,
 * removing items, and calculating totals.
 */
@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;

    // Maximum quantity limit per product in cart
    private static final int MAX_QUANTITY_PER_PRODUCT = 10;

    /**
     * Constructs a ShoppingCartServiceImpl with required dependencies.
     *
     * @param cartRepository Repository for cart operations
     * @param cartItemRepository Repository for cart item operations
     * @param productService Service for product-related operations
     */
    @Autowired
    public ShoppingCartServiceImpl(CartRepository cartRepository,
                                  CartItemRepository cartItemRepository,
                                  ProductService productService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productService = productService;
    }

    /**
     * Adds a product to a user's shopping cart. If the product already exists in the cart,
     * the quantity will be incremented. Otherwise, a new cart item is created.
     *
     * @param userId The ID of the user whose cart is being modified
     * @param productId The ID of the product to add to the cart
     * @param quantity The quantity of the product to add
     * @throws IllegalArgumentException If userId or productId is null
     * @throws ProductUnavailableException If the product does not exist
     * @throws OutOfStockException If the requested quantity exceeds available stock
     * @throws MaximumQuantityExceededException If the requested quantity exceeds the maximum allowed
     */
    @Override
    @Transactional
    public void addProductToCart(Long userId, Long productId, int quantity) {
        if (userId == null || productId == null) {
            throw new IllegalArgumentException("Invalid input parameters");
        }
        Long uid = (Long) userId;
        Cart cart = cartRepository.findByUserId(uid).orElseGet(() -> {
            Cart c = new Cart();
            c.setUserId(uid);
            return cartRepository.save(c);
        });
        Product product = productService.getProductById(productId);
        if (product == null) {
            throw new ProductUnavailableException("Product not found with id: " + productId);
        }
        if (product.getStockQuantity() < quantity) {
            throw new OutOfStockException("Product is out of stock");
        }

        Optional<CartItem> existingItem = cart.getItems().stream()
            .filter(item -> item.getProduct().getId().equals(productId))
            .findFirst();

        int newQuantity = quantity;
        if (existingItem.isPresent()) {
            newQuantity += existingItem.get().getQuantity();
        }

        // Check if the total quantity exceeds the maximum allowed
        if (newQuantity > MAX_QUANTITY_PER_PRODUCT) {
            throw new MaximumQuantityExceededException("Cannot add more than " + MAX_QUANTITY_PER_PRODUCT + " units of this product");
        }

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            newItem.setCart(cart);
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }
        cartRepository.save(cart);
    }

    /**
     * Retrieves the shopping cart for a specific user. If no cart exists,
     * a new empty cart is created for the user.
     *
     * @param userId The ID of the user whose cart is being retrieved
     * @return The user's shopping cart
     */
    @Override
    public Cart getCartForUser(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart c = new Cart();
            c.setUserId(userId);
            return cartRepository.save(c);
        });
    }

    /**
     * Updates the quantity of a product in the user's cart. If the quantity is set to
     * zero or negative, the item is removed from the cart.
     *
     * @param userId The ID of the user whose cart is being modified
     * @param productId The ID of the product to update
     * @param quantity The new quantity to set
     * @throws OutOfStockException If the new quantity exceeds available stock
     * @throws MaximumQuantityExceededException If the new quantity exceeds the maximum allowed
     */
    @Override
    public void updateProductQuantity(Long userId, Long productId, int quantity) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null) {
            CartItem toRemove = null;
            for (CartItem item : cart.getItems()) {
                if (item.getProduct().getId().equals(productId)) {
                    if (quantity <= 0) {
                        toRemove = item;
                    } else {
                        Product product = item.getProduct();
                        if (product.getStockQuantity() < quantity) {
                            throw new OutOfStockException("Product is out of stock");
                        }
                        if (quantity > MAX_QUANTITY_PER_PRODUCT) {
                            throw new MaximumQuantityExceededException("Cannot add more than " + MAX_QUANTITY_PER_PRODUCT + " units of this product");
                        }
                        item.setQuantity(quantity);
                    }
                    break;
                }
            }
            if (toRemove != null) {
                cart.getItems().remove(toRemove);
            }
            cartRepository.save(cart);
        }
    }

    /**
     * Removes a specific product from the user's cart regardless of quantity.
     *
     * @param userId The ID of the user whose cart is being modified
     * @param productId The ID of the product to remove
     */
    @Override
    public void removeProductFromCart(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null) {
            cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
            cartRepository.save(cart);
        }
    }

    /**
     * Removes all items from the user's cart.
     *
     * @param userId The ID of the user whose cart is being cleared
     */
    @Override
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null) {
            cart.clear();
            cartRepository.save(cart);
        }
    }

    /**
     * Calculates the subtotal of all items in the user's cart.
     * The subtotal is the sum of (product price × quantity) for each item.
     *
     * @param userId The ID of the user whose cart subtotal is being calculated
     * @return The cart subtotal as a double
     */
    public double getCartSubtotal(Long userId) {
        Cart cart = getCartForUser(userId);
        return cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                .doubleValue();
    }

    /**
     * Calculates the total cost of all items in the user's cart.
     * Currently this is the same as the subtotal, but could be extended to include
     * taxes, discounts, etc.
     *
     * @param userId The ID of the user whose cart total is being calculated
     * @return The cart total as a double
     */
    public double getCartTotal(Long userId) {
        Cart cart = getCartForUser(userId);
        java.math.BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        return subtotal.doubleValue();
    }

    /**
     * Persists the cart data to ensure it's saved in the database.
     * This is a minimal implementation that relies on the repository save methods.
     *
     * @param userId The ID of the user whose cart is being persisted
     */
    public void persistCart(Object userId) {
        // Minimal: do nothing for now
    }

    /**
     * Merges a guest cart into a user cart when a guest user logs in.
     * Items that exist in both carts have their quantities combined.
     * Items that only exist in the guest cart are moved to the user cart.
     * The guest cart is cleared after merging.
     *
     * @param guestSessionId The session ID of the guest user
     * @param userId The ID of the authenticated user
     */
    public void mergeGuestCartWithUserCart(Long guestSessionId, Long userId) {
        Cart guestCart = cartRepository.findByUserId(guestSessionId).orElse(null);
        Cart userCart = cartRepository.findByUserId(userId).orElse(null);
        if (guestCart != null && userCart != null) {
            for (CartItem guestItem : guestCart.getItems()) {
                boolean merged = false;
                for (CartItem userItem : userCart.getItems()) {
                    if (userItem.getProduct().getId().equals(guestItem.getProduct().getId())) {
                        userItem.setQuantity(userItem.getQuantity() + guestItem.getQuantity());
                        merged = true;
                        break;
                    }
                }
                if (!merged) {
                    CartItem newItem = new CartItem();
                    newItem.setProduct(guestItem.getProduct());
                    newItem.setQuantity(guestItem.getQuantity());
                    newItem.setCart(userCart);
                    userCart.addItem(newItem);
                }
            }
            guestCart.clear();
            cartRepository.save(userCart);
            cartRepository.save(guestCart);
        }
    }

    /**
     * Prepares a cart for checkout by validating its contents.
     * Checks that the cart is not empty and all products are available with valid quantities.
     *
     * @param userId The ID of the user whose cart is being checked out
     * @throws EmptyCartException If the cart contains no items
     * @throws InvalidCartException If any product in the cart is invalid or has an invalid quantity
     * @throws ProductUnavailableException If any product in the cart has been deleted or disabled
     */
    public void proceedToCheckout(Long userId) {
        Cart cart = getCartForUser(userId);
        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cart is empty");
        }

        // Validate each item in the cart
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();

            // Check for null product
            if (product == null) {
                throw new InvalidCartException("Product unavailable");
            }

            // Check for valid quantity
            if (item.getQuantity() <= 0) {
                throw new InvalidCartException("Invalid quantity for product: " + product.getId());
            }

            // Verify product still exists and is active by checking with the product service
            Product currentProduct = productService.getProductById(product.getId());
            if (currentProduct == null) {
                throw new ProductUnavailableException("Product has been removed: " + product.getName());
            }

            // Check if product is disabled or inactive
            if (Boolean.FALSE.equals(currentProduct.getActive())) {
                throw new ProductUnavailableException("Product is no longer available: " + product.getName());
            }

            // Check if product still has stock
            if (currentProduct.getStockQuantity() < item.getQuantity()) {
                throw new OutOfStockException("Not enough stock for product: " + product.getName());
            }
        }
    }
}
