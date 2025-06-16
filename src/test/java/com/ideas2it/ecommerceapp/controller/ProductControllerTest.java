package com.ideas2it.ecommerceapp.controller;

import com.ideas2it.ecommerceapp.dto.ApiResponse;
import com.ideas2it.ecommerceapp.model.Product;
import com.ideas2it.ecommerceapp.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductControllerTest {

    @InjectMocks
    private ProductController productController;

    @Mock
    private ProductService productService;

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
    void testGetAllProducts_ReturnsProducts() {
        // Arrange
        List<Product> products = Arrays.asList(testProduct, new Product(2L, "Another Product", 29.99, 30));
        when(productService.getAllProducts()).thenReturn(products);

        // Act
        ResponseEntity<ApiResponse<List<Product>>> response = productController.getAllProducts();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Products retrieved successfully", response.getBody().getMessage());
        assertEquals(2, response.getBody().getData().size());
    }

    @Test
    void testGetAllProducts_ServiceThrowsException_ReturnsErrorResponse() {
        // Arrange
        when(productService.getAllProducts()).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<ApiResponse<List<Product>>> response = productController.getAllProducts();

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Database error", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testGetProductById_ExistingProduct_ReturnsProduct() {
        // Arrange
        when(productService.getProductById(1L)).thenReturn(testProduct);

        // Act
        ResponseEntity<ApiResponse<Product>> response = productController.getProductById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Product retrieved successfully", response.getBody().getMessage());
        assertEquals("Test Product", response.getBody().getData().getName());
    }

    @Test
    void testGetProductById_NonExistingProduct_ReturnsErrorResponse() {
        // Arrange
        when(productService.getProductById(99L)).thenThrow(new NoSuchElementException("Product not found with id: 99"));

        // Act
        ResponseEntity<ApiResponse<Product>> response = productController.getProductById(99L);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Product not found with id: 99", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testCreateProduct_ValidProduct_ReturnsCreatedProduct() {
        // Arrange
        Product newProduct = new Product();
        newProduct.setName("New Product");
        newProduct.setPrice(BigDecimal.valueOf(9.99));
        newProduct.setStockQuantity(10);

        when(productService.createProduct(any(Product.class))).thenReturn(newProduct);

        // Act
        ResponseEntity<ApiResponse<Product>> response = productController.createProduct(newProduct);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Product created successfully", response.getBody().getMessage());
        assertEquals("New Product", response.getBody().getData().getName());
    }

    @Test
    void testCreateProduct_ServiceThrowsException_ReturnsErrorResponse() {
        // Arrange
        Product invalidProduct = new Product();
        when(productService.createProduct(any(Product.class))).thenThrow(new IllegalArgumentException("Invalid product data"));

        // Act
        ResponseEntity<ApiResponse<Product>> response = productController.createProduct(invalidProduct);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid product data", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testUpdateProduct_ExistingProduct_ReturnsUpdatedProduct() {
        // Arrange
        Product updatedProduct = new Product();
        updatedProduct.setName("Updated Product");
        updatedProduct.setPrice(BigDecimal.valueOf(24.99));

        when(productService.updateProduct(eq(1L), any(Product.class))).thenReturn(updatedProduct);

        // Act
        ResponseEntity<ApiResponse<Product>> response = productController.updateProduct(1L, updatedProduct);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Product updated successfully", response.getBody().getMessage());
        assertEquals("Updated Product", response.getBody().getData().getName());
    }

    @Test
    void testUpdateProduct_NonExistingProduct_ReturnsErrorResponse() {
        // Arrange
        Product updatedProduct = new Product();
        when(productService.updateProduct(eq(99L), any(Product.class)))
            .thenThrow(new NoSuchElementException("Product not found with id: 99"));

        // Act
        ResponseEntity<ApiResponse<Product>> response = productController.updateProduct(99L, updatedProduct);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Product not found with id: 99", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testDeleteProduct_ExistingProduct_ReturnsSuccessResponse() {
        // Arrange
        doNothing().when(productService).deleteProduct(1L);

        // Act
        ResponseEntity<ApiResponse<Void>> response = productController.deleteProduct(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Product deleted successfully", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testDeleteProduct_NonExistingProduct_ReturnsErrorResponse() {
        // Arrange
        doThrow(new NoSuchElementException("Product not found with id: 99"))
            .when(productService).deleteProduct(99L);

        // Act
        ResponseEntity<ApiResponse<Void>> response = productController.deleteProduct(99L);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Product not found with id: 99", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void testGetLowStockProducts_ReturnsLowStockProducts() {
        // Arrange
        List<Product> lowStockProducts = Arrays.asList(
            new Product(3L, "Low Stock Product 1", 9.99, 5),
            new Product(4L, "Low Stock Product 2", 14.99, 3)
        );
        when(productService.getLowStockProducts(10)).thenReturn(lowStockProducts);

        // Act
        ResponseEntity<ApiResponse<List<Product>>> response = productController.getLowStockProducts(10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Low stock products retrieved successfully", response.getBody().getMessage());
        assertEquals(2, response.getBody().getData().size());
    }

    @Test
    void testGetLowStockProducts_ServiceThrowsException_ReturnsErrorResponse() {
        // Arrange
        when(productService.getLowStockProducts(anyInt())).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<ApiResponse<List<Product>>> response = productController.getLowStockProducts(10);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Database error", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }
}
