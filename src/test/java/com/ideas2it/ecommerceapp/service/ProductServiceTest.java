package com.ideas2it.ecommerceapp.service;

import com.ideas2it.ecommerceapp.model.Product;
import com.ideas2it.ecommerceapp.repository.ProductRepository;
import com.ideas2it.ecommerceapp.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    @InjectMocks
    private ProductServiceImpl productService;

    @Mock
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create a test product
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(BigDecimal.valueOf(19.99));
        testProduct.setStockQuantity(50);
        testProduct.setActive(true);
    }

    @Test
    void testGetAllProducts_ReturnsActiveProducts() {
        // Arrange
        List<Product> activeProducts = Arrays.asList(
            testProduct,
            new Product(2L, "Another Product", 29.99, 30)
        );
        when(productRepository.findByActiveTrue()).thenReturn(activeProducts);

        // Act
        List<Product> result = productService.getAllProducts();

        // Assert
        assertEquals(2, result.size());
        verify(productRepository).findByActiveTrue();
    }

    @Test
    void testGetProductById_ExistingProduct_ReturnsProduct() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        Product result = productService.getProductById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Test Product", result.getName());
        verify(productRepository).findById(1L);
    }

    @Test
    void testGetProductById_NonExistingProduct_ThrowsException() {
        // Arrange
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            productService.getProductById(99L);
        });
        verify(productRepository).findById(99L);
    }

    @Test
    void testGetProductById_NullId_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            productService.getProductById(null);
        });
    }

    @Test
    void testCreateProduct_SavesAndReturnsProduct() {
        // Arrange
        Product newProduct = new Product();
        newProduct.setName("New Product");
        newProduct.setPrice(BigDecimal.valueOf(9.99));
        newProduct.setStockQuantity(10);

        when(productRepository.save(any(Product.class))).thenReturn(newProduct);

        // Act
        Product result = productService.createProduct(newProduct);

        // Assert
        assertNotNull(result);
        assertEquals("New Product", result.getName());
        verify(productRepository).save(newProduct);
    }

    @Test
    void testUpdateProduct_ExistingProduct_UpdatesAllFields() {
        // Arrange
        Product updatedDetails = new Product();
        updatedDetails.setName("Updated Name");
        updatedDetails.setDescription("Updated Description");
        updatedDetails.setPrice(BigDecimal.valueOf(24.99));
        updatedDetails.setStockQuantity(75);
        updatedDetails.setImageUrl("updated-image.jpg");
        updatedDetails.setActive(true);

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Product result = productService.updateProduct(1L, updatedDetails);

        // Assert
        assertEquals("Updated Name", result.getName());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(BigDecimal.valueOf(24.99), result.getPrice());
        assertEquals(75, result.getStockQuantity());
        assertEquals("updated-image.jpg", result.getImageUrl());
        assertTrue(result.getActive());
        verify(productRepository).save(testProduct);
    }

    @Test
    void testUpdateProductStock_DecreasesStock() {
        // Arrange
        Product product = new Product(1L, "Test Product", 19.99, 50);
        when(productRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        productService.updateProductStock(1L, 10);

        // Assert
        assertEquals(40, product.getStockQuantity());
        verify(productRepository).save(product);
    }

    @Test
    void testUpdateProductStock_InsufficientStock_ThrowsException() {
        // Arrange
        Product product = new Product(1L, "Test Product", 19.99, 5);
        when(productRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            productService.updateProductStock(1L, 10);
        });
    }

    @Test
    void testDeleteProduct_SetsActiveToFalse() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        productService.deleteProduct(1L);

        // Assert
        assertFalse(testProduct.getActive());
        verify(productRepository).save(testProduct);
    }

    @Test
    void testGetLowStockProducts_ReturnsProductsBelowThreshold() {
        // Arrange
        List<Product> lowStockProducts = Arrays.asList(
            new Product(3L, "Low Stock Product 1", 9.99, 5),
            new Product(4L, "Low Stock Product 2", 14.99, 3)
        );
        when(productRepository.findByStockQuantityLessThan(10)).thenReturn(lowStockProducts);

        // Act
        List<Product> result = productService.getLowStockProducts(10);

        // Assert
        assertEquals(2, result.size());
        verify(productRepository).findByStockQuantityLessThan(10);
    }
}
