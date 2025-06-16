package com.ideas2it.ecommerceapp.service.impl;

import jakarta.transaction.Transactional;
import com.ideas2it.ecommerceapp.model.Product;
import com.ideas2it.ecommerceapp.repository.ProductRepository;
import com.ideas2it.ecommerceapp.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Implementation of the ProductService interface that provides functionality
 * for managing products in the ecommerce application.
 * This service handles operations like retrieving, creating, updating, and
 * soft-deleting products.
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    /**
     * Retrieves all active products from the database.
     *
     * @return A list of all active products
     */
    @Override
    public List<Product> getAllProducts() {
        return productRepository.findByActiveTrue();
    }

    /**
     * Retrieves a specific product by its ID.
     *
     * @param id The ID of the product to retrieve
     * @return The product with the specified ID
     * @throws IllegalArgumentException If the product ID is null
     * @throws NoSuchElementException If no product with the specified ID exists
     */
    @Override
    public Product getProductById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        return productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + id));
    }

    /**
     * Creates a new product in the database.
     *
     * @param product The product entity to be created
     * @return The created product with assigned ID
     */
    @Override
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    /**
     * Updates an existing product with new details.
     *
     * @param id The ID of the product to update
     * @param productDetails The product entity containing updated details
     * @return The updated product
     * @throws NoSuchElementException If no product with the specified ID exists
     */
    @Override
    public Product updateProduct(Long id, Product productDetails) {
        Product product = getProductById(id);
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setImageUrl(productDetails.getImageUrl());
        product.setActive(productDetails.getActive());
        return productRepository.save(product);
    }

    /**
     * Updates the stock quantity of a product by reducing it by the specified amount.
     * Uses pessimistic locking to prevent concurrent updates leading to overselling.
     *
     * @param productId The ID of the product whose stock needs to be updated
     * @param quantity The quantity to be reduced from the current stock
     * @throws NoSuchElementException If no product with the specified ID exists
     * @throws IllegalStateException If there is insufficient stock available
     */
    @Override
    @Transactional
    public void updateProductStock(Long productId, int quantity) {
        // Using pessimistic lock to prevent concurrent updates to stock
        Optional<Product> productOpt = productRepository.findByIdWithPessimisticLock(productId);
        Product product = productOpt.orElseThrow(() ->
            new NoSuchElementException("Product not found with id: " + productId));

        int newQuantity = product.getStockQuantity() - quantity;
        if (newQuantity < 0) {
            throw new IllegalStateException("Insufficient stock for product: " + product.getName());
        }

        product.setStockQuantity(newQuantity);
        productRepository.save(product);
    }

    /**
     * Soft deletes a product by setting its active status to false.
     * The product remains in the database but won't be returned in normal queries.
     *
     * @param id The ID of the product to delete
     * @throws NoSuchElementException If no product with the specified ID exists
     */
    @Override
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    /**
     * Retrieves all products whose stock quantity is below a specified threshold.
     * Useful for inventory management and reordering alerts.
     *
     * @param threshold The stock level below which products should be returned
     * @return A list of products with stock quantity below the threshold
     */
    @Override
    public List<Product> getLowStockProducts(int threshold) {
        return productRepository.findByStockQuantityLessThan(threshold);
    }
}
