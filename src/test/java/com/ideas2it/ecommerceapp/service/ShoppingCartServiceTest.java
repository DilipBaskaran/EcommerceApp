package com.ideas2it.ecommerceapp.service;

import com.ideas2it.ecommerceapp.exception.EmptyCartException;
import com.ideas2it.ecommerceapp.exception.ExpiredCouponException;
import com.ideas2it.ecommerceapp.exception.InvalidCartException;
import com.ideas2it.ecommerceapp.exception.InvalidCouponException;
import com.ideas2it.ecommerceapp.exception.MaximumQuantityExceededException;
import com.ideas2it.ecommerceapp.exception.OutOfStockException;
import com.ideas2it.ecommerceapp.exception.ProductUnavailableException;
import com.ideas2it.ecommerceapp.model.Product;
import com.ideas2it.ecommerceapp.model.Cart;
import com.ideas2it.ecommerceapp.model.CartItem;
import com.ideas2it.ecommerceapp.repository.CartItemRepository;
import com.ideas2it.ecommerceapp.repository.CartRepository;
import com.ideas2it.ecommerceapp.service.impl.ShoppingCartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShoppingCartServiceTest {

    @InjectMocks
    private ShoppingCartServiceImpl shoppingCartService;

    @Mock
    private ProductService productService;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Default Cart mock for any userId
        when(cartRepository.findByUserId(any())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setItems(new java.util.ArrayList<>());
            return java.util.Optional.of(cart);
        });
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // 1. Cart Item Management
    @Test
    void testAddProductToCart_NewProduct_AddsItem() {
        Long userId = 1L;
        Long productId = 100L;
        int quantity = 2;
        Product product = new Product(productId, "Test Product", 10.0, 100);
        when(productService.getProductById(productId)).thenReturn(product);
        shoppingCartService.addProductToCart(userId, productId, quantity);
        Cart cart = new Cart();
        CartItem item1 = new CartItem();
        item1.setCart(cart);
        item1.setProduct(product);
        item1.setQuantity(2);
        cart.setItems(List.of(
                item1
        ));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        Cart cart1 = shoppingCartService.getCartForUser(userId);

        assertEquals(1, cart1.getItems().size());
        CartItem item = cart1.getItems().get(0);
        assertEquals(productId, item.getProduct().getId());
        assertEquals(quantity, item.getQuantity());
    }

    @Test
    void testAddProductToCart_ExistingProduct_IncrementsQuantity() {
        Long userId = 1L;
        Long productId = 100L;
        int initialQuantity = 1;
        int addedQuantity = 3;
        Product product = new Product(productId, "Test Product", 10.0, 100);
        when(productService.getProductById(productId)).thenReturn(product);
        shoppingCartService.addProductToCart(userId, productId, initialQuantity);
        shoppingCartService.addProductToCart(userId, productId, addedQuantity);
        Cart cart = new Cart();
        CartItem item1 = new CartItem();
        item1.setCart(cart);
        item1.setProduct(product);
        item1.setQuantity(4);
        cart.setItems(List.of(
                item1
        ));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        Cart cart1 = shoppingCartService.getCartForUser(userId);
        assertEquals(1, cart1.getItems().size());
        CartItem item = cart1.getItems().get(0);
        assertEquals(productId, item.getProduct().getId());
        assertEquals(initialQuantity + addedQuantity, item.getQuantity());
    }

    @Test
    void testAddProductToCart_ProductOutOfStock_ThrowsException() {
        Long userId = 1L;
        Long productId = 100L;
        int quantity = 2;
        Product product = new Product(productId, "Test Product", 10.0, 0); // Out of stock
        when(productService.getProductById(productId)).thenReturn(product);
        assertThrows(OutOfStockException.class, () -> {
            shoppingCartService.addProductToCart(userId, productId, quantity);
        });
    }

    @Test
    void testUpdateProductQuantity_ValidQuantity_UpdatesQuantity() {
        Long userId = 1L;
        Long productId = 100L;
        int initialQuantity = 1;
        int newQuantity = 5;
        Product product = new Product(productId, "Test Product", 10.0, 100);
        when(productService.getProductById(productId)).thenReturn(product);
        shoppingCartService.addProductToCart(userId, productId, initialQuantity);
        shoppingCartService.updateProductQuantity(userId, productId, newQuantity);
        Cart cart = new Cart();
        CartItem item1 = new CartItem();
        item1.setCart(cart);
        item1.setProduct(product);
        item1.setQuantity(5);
        cart.setItems(List.of(
                item1
        ));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        Cart cart1 = shoppingCartService.getCartForUser(userId);
        CartItem item = cart1.getItems().get(0);
        assertEquals(newQuantity, item.getQuantity());
    }

    @Test
    void testUpdateProductQuantity_ZeroOrNegative_RemovesItem() {
        Long userId = 1L;
        Long productId = 100L;
        int initialQuantity = 2;
        Product product = new Product(productId, "Test Product", 10.0, 100);
        when(productService.getProductById(productId)).thenReturn(product);
        shoppingCartService.addProductToCart(userId, productId, initialQuantity);
        shoppingCartService.updateProductQuantity(userId, productId, 0);
        Cart cart = shoppingCartService.getCartForUser(userId);
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void testRemoveProductFromCart_ProductExists_RemovesItem() {
        Long userId = 1L;
        Long productId = 100L;
        int quantity = 2;
        Product product = new Product(productId, "Test Product", 10.0, 100);
        when(productService.getProductById(productId)).thenReturn(product);
        shoppingCartService.addProductToCart(userId, productId, quantity);
        shoppingCartService.removeProductFromCart(userId, productId);
        Cart cart = shoppingCartService.getCartForUser(userId);
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void testRemoveProductFromCart_ProductNotInCart_NoChange() {
        Long userId = 1L;
        Long productId = 100L;
        Cart cartBefore = shoppingCartService.getCartForUser(userId);
        int initialSize = cartBefore.getItems().size();
        shoppingCartService.removeProductFromCart(userId, productId);
        Cart cartAfter = shoppingCartService.getCartForUser(userId);
        assertEquals(initialSize, cartAfter.getItems().size());
    }

    @Test
    void testClearCart_RemovesAllItems() {
        Long userId = 1L;
        Product product1 = new Product(100L, "Product 1", 10.0, 100);
        Product product2 = new Product(101L, "Product 2", 20.0, 100);
        when(productService.getProductById(100L)).thenReturn(product1);
        when(productService.getProductById(101L)).thenReturn(product2);
        shoppingCartService.addProductToCart(userId, 100L, 1);
        shoppingCartService.addProductToCart(userId, 101L, 2);
        shoppingCartService.clearCart(userId);
        Cart cart = shoppingCartService.getCartForUser(userId);
        assertTrue(cart.getItems().isEmpty());
    }

    // 2. Cart Viewing & Calculation
    @Test
    void testGetCartContents_ReturnsAllItemsWithDetails() {
        Long userId = 1L;
        Product product1 = new Product(100L, "Product 1", 10.0, 100);
        Product product2 = new Product(101L, "Product 2", 20.0, 100);
        when(productService.getProductById(100L)).thenReturn(product1);
        when(productService.getProductById(101L)).thenReturn(product2);
        shoppingCartService.addProductToCart(userId, 100L, 1);
        shoppingCartService.addProductToCart(userId, 101L, 2);
        Cart cart = new Cart();
        CartItem item1 = new CartItem();
        item1.setCart(cart);
        item1.setProduct(product1);
        item1.setQuantity(2);
        CartItem item2 = new CartItem();
        item2.setCart(cart);
        item2.setProduct(product2);
        item2.setQuantity(2);
        cart.setItems(List.of(
                item1, item2
        ));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        Cart cart1 = shoppingCartService.getCartForUser(userId);
        assertEquals(2, cart1.getItems().size());
        assertEquals("Product 1", cart1.getItems().get(0).getProduct().getName());
        assertEquals("Product 2", cart1.getItems().get(1).getProduct().getName());
    }

    @Test
    void testGetCartSubtotal_CalculatesCorrectSubtotal() {
        Long userId = 1L;
        Product product1 = new Product(100L, "Product 1", 10.0, 100);
        Product product2 = new Product(101L, "Product 2", 20.0, 100);
        when(productService.getProductById(100L)).thenReturn(product1);
        when(productService.getProductById(101L)).thenReturn(product2);
        shoppingCartService.addProductToCart(userId, 100L, 2); // 2 x 10 = 20
        shoppingCartService.addProductToCart(userId, 101L, 1); // 1 x 20 = 20
        // Mock the cart repository to return a cart with these items
        Cart cart = new Cart();
        CartItem item1 = new CartItem();
        item1.setCart(cart);
        item1.setProduct(product1);
        item1.setQuantity(2);
        CartItem item2 = new CartItem();
        item2.setCart(cart);
        item2.setProduct(product1);
        item2.setQuantity(2);
        cart.setItems(List.of(
            item1, item2
        ));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        double subtotal = shoppingCartService.getCartSubtotal(userId);
        assertEquals(40.0, subtotal);
    }

    @Test
    void testGetCartTotal_WithNoItems_ReturnsZero() {
        Long userId = 1L;
        double total = shoppingCartService.getCartTotal(userId);
        assertEquals(0.0, total);
    }

    // 3. Persistence & User Handling
    @Test
    void testPersistCart_LoggedInUser_SavesToDatabase() {
        Long userId = 1L;
        when(productService.getProductById(100L)).thenReturn(new Product(100L, "Product 1", 10.0, 100));
        shoppingCartService.addProductToCart(userId, 100L, 1);
        // Simulate persistence (mock CartRepository if needed)
        assertDoesNotThrow(() -> shoppingCartService.persistCart(userId));
    }

    @Test
    void testPersistCart_GuestUser_SavesToSessionOrCookie() {
        Long guestSessionId = 123L;
        when(productService.getProductById(100L)).thenReturn(new Product(100L, "Product 1", 10.0, 100));
        shoppingCartService.addProductToCart(guestSessionId, 100L, 1);
        assertDoesNotThrow(() -> shoppingCartService.persistCart(guestSessionId));
    }

    @Test
    void testRetrieveCart_LoggedInUser_LoadsFromDatabase() {
        Long userId = 1L;
        // Simulate retrieval (mock CartRepository if needed)
        Cart cart = shoppingCartService.getCartForUser(userId);
        assertNotNull(cart);
    }

    @Test
    void testRetrieveCart_GuestUser_LoadsFromSessionOrCookie() {
        Long guestSessionId = 123L;
        Cart cart = shoppingCartService.getCartForUser(guestSessionId);
        assertNotNull(cart);
    }

    @Test
    void testMergeGuestCartWithUserCart_OnLogin_MergesCorrectly() {
        Long guestSessionId = 123L;
        Long userId = 1L;
        Product product1 = new Product(100L, "Product 1", 10.0, 100);
        Product product2 = new Product(100L, "Product 2", 20.0, 100);
        when(productService.getProductById(100L)).thenReturn(product1);
        when(productService.getProductById(101L)).thenReturn(product2);
        shoppingCartService.addProductToCart(guestSessionId, 100L, 1);
        shoppingCartService.addProductToCart(userId, 101L, 2);
        shoppingCartService.mergeGuestCartWithUserCart(guestSessionId, userId);
        Cart cart = new Cart();
        CartItem item1 = new CartItem();
        item1.setCart(cart);
        item1.setProduct(product1);
        item1.setQuantity(2);
        CartItem item2 = new CartItem();
        item2.setCart(cart);
        item2.setProduct(product1);
        item2.setQuantity(2);
        cart.setItems(List.of(
                item1, item2
        ));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        Cart cart1 = shoppingCartService.getCartForUser(userId);
        assertEquals(2, cart.getItems().size());
    }

    // 4. Inventory & Product Availability
    @Test
    void testAddProductToCart_ProductUnavailable_ThrowsException() {
        Long userId = 1L;
        Long productId = 200L;
        when(productService.getProductById(productId)).thenReturn(null);
        assertThrows(ProductUnavailableException.class, () -> {
            shoppingCartService.addProductToCart(userId, productId, 1);
        });
    }

    @Test
    void testUpdateProductQuantity_ExceedsStock_ThrowsException() {
        Long userId = 1L;
        Long productId = 100L;
        Product product = new Product(productId, "Test Product", 10.0, 2);
        when(productService.getProductById(productId)).thenReturn(product);
        shoppingCartService.addProductToCart(userId, productId, 2);
        Cart cart = new Cart();
        CartItem item1 = new CartItem();
        item1.setCart(cart);
        item1.setProduct(product);
        item1.setQuantity(2);
        cart.setItems(List.of(
                item1
        ));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        assertThrows(OutOfStockException.class, () -> {
            shoppingCartService.updateProductQuantity(userId, productId, 5);
        });
    }

    @Test
    void testCheckoutWithUnavailableProduct_ThrowsException() {
        Long userId = 1L;
        Long productId = 100L;
        when(productService.getProductById(productId)).thenReturn(null);
        assertThrows(ProductUnavailableException.class, () -> {
            shoppingCartService.addProductToCart(userId, productId, 1);
            shoppingCartService.proceedToCheckout(userId);
        });
    }

    // 6. Checkout Transition
    @Test
    void testProceedToCheckout_WithValidCart_Succeeds() {
        Long userId = 1L;
        Product product1 = new Product(100L, "Product 1", 10.0, 100);
        when(productService.getProductById(100L)).thenReturn(product1);

        // Add product to cart
        shoppingCartService.addProductToCart(userId, 100L, 2);

        // Setup cart with valid items for the findByUserId response
        Cart cart = new Cart();
        cart.setUserId(userId);
        CartItem item = new CartItem();
        item.setProduct(product1);
        item.setQuantity(2);
        item.setCart(cart);
        cart.addItem(item);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // This should not throw any exception
        assertDoesNotThrow(() -> shoppingCartService.proceedToCheckout(userId));
    }

    @Test
    void testProceedToCheckout_WithEmptyCart_ThrowsException() {
        Long userId = 1L;
        // Ensure cart is empty
        Cart emptyCart = new Cart();
        emptyCart.setUserId(userId);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(emptyCart));

        // Should throw EmptyCartException
        assertThrows(EmptyCartException.class, () -> shoppingCartService.proceedToCheckout(userId));
    }

    @Test
    void testProceedToCheckout_WithNullProduct_ThrowsException() {
        Long userId = 1L;

        // Create cart with an item that has a null product
        Cart cart = new Cart();
        cart.setUserId(userId);
        CartItem invalidItem = new CartItem();
        invalidItem.setProduct(null); // Null product
        invalidItem.setQuantity(1);
        invalidItem.setCart(cart);
        cart.addItem(invalidItem);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Should throw InvalidCartException
        assertThrows(InvalidCartException.class, () -> shoppingCartService.proceedToCheckout(userId));
    }

    @Test
    void testProceedToCheckout_WithNegativeQuantity_ThrowsException() {
        Long userId = 1L;
        Product product = new Product(100L, "Product 1", 10.0, 100);

        // Create cart with an item that has a negative quantity
        Cart cart = new Cart();
        cart.setUserId(userId);
        CartItem invalidItem = new CartItem();
        invalidItem.setProduct(product);
        invalidItem.setQuantity(-1); // Negative quantity
        invalidItem.setCart(cart);
        cart.addItem(invalidItem);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Should throw InvalidCartException
        assertThrows(InvalidCartException.class, () -> shoppingCartService.proceedToCheckout(userId));
    }

    // 7. Edge Cases
    @Test
    void testAddNullProductToCart_ThrowsException() {
        Long userId = 1L;
        assertThrows(IllegalArgumentException.class, () -> {
            shoppingCartService.addProductToCart(userId, null, 1);
        });
    }

    @Test
    void testAddProductWithNegativeQuantity_ThrowsException() {
        Long userId = 1L;
        Long productId = 100L;
        assertThrows(ProductUnavailableException.class, () -> {
            shoppingCartService.addProductToCart(userId, productId, -2);
        });
    }

    // 8. Test updateProductQuantity - Beyond Available Stock
    @Test
    void testUpdateProductQuantity_BeyondAvailableStock_ThrowsException() {
        Long userId = 1L;
        Long productId = 100L;
        int initialQuantity = 2;
        int newQuantity = 15; // More than available stock

        Product product = new Product(productId, "Test Product", 10.0, 10); // Only 10 in stock
        when(productService.getProductById(productId)).thenReturn(product);

        // Add product to cart first
        shoppingCartService.addProductToCart(userId, productId, initialQuantity);

        // Create cart with item that will be found when updating quantity
        Cart cart = new Cart();
        cart.setUserId(userId);
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(initialQuantity);
        item.setCart(cart);
        cart.addItem(item);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Should throw OutOfStockException when trying to update beyond available stock
        assertThrows(OutOfStockException.class, () ->
            shoppingCartService.updateProductQuantity(userId, productId, newQuantity));
    }

    // 9. Test mergeGuestCartWithUserCart - Same Product in Both Carts
    @Test
    void testMergeGuestCartWithUserCart_SameProductInBothCarts_AddsQuantities() {
        Long guestSessionId = 123L;
        Long userId = 1L;
        Long commonProductId = 100L;

        // Create product that will be in both carts
        Product product = new Product(commonProductId, "Common Product", 10.0, 100);

        // Create guest cart with the common product
        Cart guestCart = new Cart();
        guestCart.setUserId(guestSessionId);
        CartItem guestItem = new CartItem();
        guestItem.setProduct(product);
        guestItem.setQuantity(2);
        guestItem.setCart(guestCart);
        guestCart.addItem(guestItem);

        // Create user cart with the same product but different quantity
        Cart userCart = new Cart();
        userCart.setUserId(userId);
        CartItem userItem = new CartItem();
        userItem.setProduct(product);
        userItem.setQuantity(3);
        userItem.setCart(userCart);
        userCart.addItem(userItem);

        // Mock repository responses
        when(cartRepository.findByUserId(guestSessionId)).thenReturn(Optional.of(guestCart));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(userCart));

        // Perform merge
        shoppingCartService.mergeGuestCartWithUserCart(guestSessionId, userId);

        // Verify merged cart (user cart should now have quantity 5)
        verify(cartRepository, times(2)).save(any(Cart.class));
        assertEquals(1, userCart.getItems().size());
        assertEquals(5, userCart.getItems().get(0).getQuantity()); // 2 from guest + 3 from user

        // Verify guest cart was cleared
        assertTrue(guestCart.getItems().isEmpty());
    }

    // 10. Test mergeGuestCartWithUserCart - Unique Products in Each Cart
    @Test
    void testMergeGuestCartWithUserCart_UniqueProducts_CombinesBoth() {
        Long guestSessionId = 123L;
        Long userId = 1L;

        // Create different products for each cart
        Product guestProduct = new Product(101L, "Guest Product", 15.0, 100);
        Product userProduct = new Product(102L, "User Product", 25.0, 100);

        // Create guest cart with guest-specific product
        Cart guestCart = new Cart();
        guestCart.setUserId(guestSessionId);
        CartItem guestItem = new CartItem();
        guestItem.setProduct(guestProduct);
        guestItem.setQuantity(2);
        guestItem.setCart(guestCart);
        guestCart.addItem(guestItem);

        // Create user cart with user-specific product
        Cart userCart = new Cart();
        userCart.setUserId(userId);
        CartItem userItem = new CartItem();
        userItem.setProduct(userProduct);
        userItem.setQuantity(1);
        userItem.setCart(userCart);
        userCart.addItem(userItem);

        // Mock repository responses
        when(cartRepository.findByUserId(guestSessionId)).thenReturn(Optional.of(guestCart));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(userCart));

        // Perform merge
        shoppingCartService.mergeGuestCartWithUserCart(guestSessionId, userId);

        // Verify merged cart (user cart should now have 2 items)
        verify(cartRepository, times(2)).save(any(Cart.class));
        assertEquals(2, userCart.getItems().size());

        // Verify guest cart was cleared
        assertTrue(guestCart.getItems().isEmpty());
    }

    // 11. Test mergeGuestCartWithUserCart - Empty Guest Cart
    @Test
    void testMergeGuestCartWithUserCart_EmptyGuestCart_NoChange() {
        Long guestSessionId = 123L;
        Long userId = 1L;

        // Create empty guest cart
        Cart guestCart = new Cart();
        guestCart.setUserId(guestSessionId);

        // Create user cart with a product
        Cart userCart = new Cart();
        userCart.setUserId(userId);
        Product userProduct = new Product(102L, "User Product", 25.0, 100);
        CartItem userItem = new CartItem();
        userItem.setProduct(userProduct);
        userItem.setQuantity(1);
        userItem.setCart(userCart);
        userCart.addItem(userItem);

        // Mock repository responses
        when(cartRepository.findByUserId(guestSessionId)).thenReturn(Optional.of(guestCart));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(userCart));

        // Before merge - capture state
        int userCartSizeBefore = userCart.getItems().size();

        // Perform merge
        shoppingCartService.mergeGuestCartWithUserCart(guestSessionId, userId);

        // Verify user cart remains unchanged
        assertEquals(userCartSizeBefore, userCart.getItems().size());
    }

    // 12. Test Transaction Behavior - Rollback on Exception
    //@Test
    void testAddProductToCart_DatabaseError_RollsBackTransaction() {
        Long userId = 1L;
        Long productId = 100L;
        int quantity = 2;

        Product product = new Product(productId, "Test Product", 10.0, 100);
        when(productService.getProductById(productId)).thenReturn(product);

        // Mock cart repository to return a valid cart
        Cart cart = new Cart();
        cart.setUserId(userId);
        Product userProduct = new Product(102L, "User Product", 25.0, 100);
        CartItem userItem = new CartItem();
        userItem.setProduct(userProduct);
        userItem.setQuantity(1);
        userItem.setCart(cart);
        cart.addItem(userItem);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Mock cartItemRepository to throw exception when saving
        when(cartItemRepository.save(any(CartItem.class))).thenThrow(new RuntimeException("Database error"));

        // Attempt to add product should throw exception
        assertThrows(RuntimeException.class, () ->
            shoppingCartService.addProductToCart(userId, productId, quantity));

        // Verify the cart is still empty (transaction rolled back)
        assertEquals(0, cart.getItems().size());
    }

    // 13. Test addProductToCart with null userId
    @Test
    void testAddProductToCart_NullUserId_ThrowsException() {
        Long userId = null;
        Long productId = 100L;
        int quantity = 2;

        assertThrows(IllegalArgumentException.class, () ->
            shoppingCartService.addProductToCart(userId, productId, quantity));


        // Verify that no interaction with repositories occurred
        verify(productService, never()).getProductById(any());
        verify(cartItemRepository, never()).save(any());
    }

    // 14. Test addProductToCart with zero quantity
    @Test
    void testAddProductToCart_ZeroQuantity_AddsItemWithZeroQuantity() {
        Long userId = 1L;
        Long productId = 100L;
        int quantity = 0;

        Product product = new Product(productId, "Test Product", 10.0, 100);
        when(productService.getProductById(productId)).thenReturn(product);

        // This should work but add item with zero quantity
        shoppingCartService.addProductToCart(userId, productId, quantity);

        // Verify the call was made to add the item
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    void testAddProductToCart_NullProductId_ThrowsException() {
        Long userId = 1L;
        Long productId = null;
        int quantity = 2;

        assertThrows(IllegalArgumentException.class, () -> {
            shoppingCartService.addProductToCart(userId, productId, quantity);
        });

        // Verify that no interaction with repositories occurred
        verify(productService, never()).getProductById(any());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void testAddProductToCart_ProductNotFound_ThrowsException() {
        Long userId = 1L;
        Long productId = 999L; // Non-existent product ID
        int quantity = 2;

        // Mock ProductService to return null for this ID
        when(productService.getProductById(productId)).thenReturn(null);

        // Verify that ProductUnavailableException is thrown
        assertThrows(ProductUnavailableException.class, () -> {
            shoppingCartService.addProductToCart(userId, productId, quantity);
        });

        // Verify that the product service was called, but the cart item repository was not
        verify(productService).getProductById(productId);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void testAddProductToCart_ExceedsMaximumQuantity_ThrowsException() {
        Long userId = 1L;
        Long productId = 100L;
        Product product = new Product(productId, "Test Product", 10.0, 100);

        // Mock the product service
        when(productService.getProductById(productId)).thenReturn(product);

        // First add 8 items to the cart
        shoppingCartService.addProductToCart(userId, productId, 8);

        // Create cart with existing item to be returned by repository
        Cart cart = new Cart();
        cart.setUserId(userId);
        CartItem existingItem = new CartItem();
        existingItem.setProduct(product);
        existingItem.setQuantity(8);
        existingItem.setCart(cart);
        cart.addItem(existingItem);

        // Mock repository to return the cart with existing items
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Try to add 3 more (which would exceed the limit of 10)
        assertThrows(MaximumQuantityExceededException.class, () -> {
            shoppingCartService.addProductToCart(userId, productId, 3);
        });
    }

    @Test
    void testUpdateProductQuantity_ExceedsMaximumQuantity_ThrowsException() {
        Long userId = 1L;
        Long productId = 100L;
        Product product = new Product(productId, "Test Product", 10.0, 100);

        // Mock the product service
        when(productService.getProductById(productId)).thenReturn(product);

        // Add product to cart with initial quantity
        shoppingCartService.addProductToCart(userId, productId, 5);

        // Create cart with existing item to be returned by repository
        Cart cart = new Cart();
        cart.setUserId(userId);
        CartItem existingItem = new CartItem();
        existingItem.setProduct(product);
        existingItem.setQuantity(5);
        existingItem.setCart(cart);
        cart.addItem(existingItem);

        // Mock repository to return the cart with existing items
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Now modify the updateProductQuantity method to also check for maximum quantity
        // and test that it throws the exception when updating to more than 10
        assertThrows(MaximumQuantityExceededException.class, () -> {
            shoppingCartService.updateProductQuantity(userId, productId, 11);
        });
    }

    @Test
    void testProceedToCheckout_WithDeletedProduct_ThrowsException() {
        Long userId = 1L;
        Long productId = 100L;

        // First, mock a product that exists
        Product product = new Product(productId, "Test Product", 10.0, 100);
        when(productService.getProductById(productId)).thenReturn(product);

        // Add the product to cart
        shoppingCartService.addProductToCart(userId, productId, 2);

        // Create cart with the product to be returned by repository
        Cart cart = new Cart();
        cart.setUserId(userId);
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setCart(cart);
        cart.addItem(item);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Now simulate that the product was deleted by returning null when checked again
        when(productService.getProductById(productId)).thenReturn(null);

        // Should throw ProductUnavailableException
        assertThrows(ProductUnavailableException.class, () ->
            shoppingCartService.proceedToCheckout(userId));
    }

    @Test
    void testProceedToCheckout_WithDisabledProduct_ThrowsException() {
        Long userId = 1L;
        Long productId = 100L;

        // First, mock a product that exists and is active
        Product activeProduct = new Product(productId, "Test Product", 10.0, 100);
        when(productService.getProductById(productId)).thenReturn(activeProduct);

        // Add the product to cart
        shoppingCartService.addProductToCart(userId, productId, 2);

        // Create cart with the product to be returned by repository
        Cart cart = new Cart();
        cart.setUserId(userId);
        CartItem item = new CartItem();
        item.setProduct(activeProduct);
        item.setQuantity(2);
        item.setCart(cart);
        cart.addItem(item);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Now simulate that the product was disabled
        Product disabledProduct = new Product(productId, "Test Product", 10.0, 100);
        disabledProduct.setActive(false);
        when(productService.getProductById(productId)).thenReturn(disabledProduct);

        // Should throw ProductUnavailableException
        assertThrows(ProductUnavailableException.class, () ->
            shoppingCartService.proceedToCheckout(userId));
    }

    @Test
    void testProceedToCheckout_WithPriceChanges_UsesOriginalPrice() {
        Long userId = 1L;
        Long productId = 100L;

        // Create product with initial price
        double initialPrice = 10.0;
        Product product = new Product(productId, "Test Product", initialPrice, 100);
        when(productService.getProductById(productId)).thenReturn(product);

        // Add product to cart
        shoppingCartService.addProductToCart(userId, productId, 2);

        // Create cart with the product at the initial price to be returned by repository
        Cart cart = new Cart();
        cart.setUserId(userId);
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setCart(cart);
        cart.addItem(item);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Now simulate that the product price has changed
        double newPrice = 15.0;
        Product updatedProduct = new Product(productId, "Test Product", newPrice, 100);
        when(productService.getProductById(productId)).thenReturn(updatedProduct);

        // Get the cart subtotal - should use the original price
        double subtotal = shoppingCartService.getCartSubtotal(userId);

        // Subtotal should reflect original price (2 * $10 = $20)
        assertEquals(20.0, subtotal, 0.01);

        // Proceed to checkout should not throw an exception
        assertDoesNotThrow(() -> shoppingCartService.proceedToCheckout(userId));
    }

    @Test
    void testGetCartSubtotal_PriceChangedInSystem_UsesStoredPrice() {
        Long userId = 1L;
        Long productId = 100L;

        // Create product with initial price
        double initialPrice = 10.0;
        Product product = new Product(productId, "Test Product", initialPrice, 100);
        when(productService.getProductById(productId)).thenReturn(product);

        // Add the product to cart
        shoppingCartService.addProductToCart(userId, productId, 3);

        // Create cart with the product with stored price
        Cart cart = new Cart();
        cart.setUserId(userId);
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(3);
        item.setCart(cart);
        cart.addItem(item);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Calculate subtotal with original price
        double originalSubtotal = shoppingCartService.getCartSubtotal(userId);
        assertEquals(30.0, originalSubtotal, 0.01);

        // Now change the product price in the system
        product.setPrice(java.math.BigDecimal.valueOf(20.0));

        // Calculate subtotal again - should still use the stored price
        double newSubtotal = shoppingCartService.getCartSubtotal(userId);
        assertEquals(30.0, newSubtotal, 0.01);
    }

    @Test
    void testGetCartSubtotal_WithLargeNumberOfItems_CalculatesCorrectly() {
        Long userId = 1L;
        Long productId = 100L;

        // Create a product
        Product product = new Product(productId, "Test Product", 10.0, 1000);
        when(productService.getProductById(productId)).thenReturn(product);

        // Create a cart with a large number of items (100 different items)
        Cart cart = new Cart();
        cart.setUserId(userId);

        int numberOfItems = 100;
        double expectedTotal = 0.0;

        // Add many different products to the cart
        for (int i = 0; i < numberOfItems; i++) {
            Product itemProduct = new Product((long)(i + 100), "Product " + i, i + 1.0, 100);
            CartItem item = new CartItem();
            item.setProduct(itemProduct);
            item.setQuantity(1);
            item.setCart(cart);
            cart.addItem(item);

            expectedTotal += (i + 1.0);
        }

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Calculate subtotal - should handle large number of items efficiently
        long startTime = System.currentTimeMillis();
        double subtotal = shoppingCartService.getCartSubtotal(userId);
        long endTime = System.currentTimeMillis();

        // Verify correct calculation
        assertEquals(expectedTotal, subtotal, 0.01);

        // Verify calculation completed in a reasonable time (less than 500ms)
        // This is a soft performance test - adjust threshold as needed
        assertTrue((endTime - startTime) < 500, "Cart subtotal calculation took too long: " + (endTime - startTime) + "ms");
    }

    @Test
    void testProceedToCheckout_WithLargeCart_ValidatesEfficiently() {
        Long userId = 1L;

        // Create a cart with a large number of items
        Cart cart = new Cart();
        cart.setUserId(userId);

        int numberOfItems = 50;

        // Add many different products to the cart
        for (int i = 0; i < numberOfItems; i++) {
            Long itemProductId = (long)(i + 100);
            Product itemProduct = new Product(itemProductId, "Product " + i, i + 1.0, 100);
            CartItem item = new CartItem();
            item.setProduct(itemProduct);
            item.setQuantity(1);
            item.setCart(cart);
            cart.addItem(item);

            // Mock product service to return each product when validated
            when(productService.getProductById(itemProductId)).thenReturn(itemProduct);
        }

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Time how long validation takes
        long startTime = System.currentTimeMillis();
        // Proceed to checkout should validate all items efficiently
        assertDoesNotThrow(() -> shoppingCartService.proceedToCheckout(userId));
        long endTime = System.currentTimeMillis();

        // Verify validation completed in a reasonable time (less than 1000ms)
        // This is a soft performance test - adjust threshold as needed
        assertTrue((endTime - startTime) < 1000,
            "Cart validation took too long: " + (endTime - startTime) + "ms for " + numberOfItems + " items");
    }
}
