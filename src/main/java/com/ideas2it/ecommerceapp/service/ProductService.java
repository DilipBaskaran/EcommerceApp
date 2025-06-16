package com.ideas2it.ecommerceapp.service;

import com.ideas2it.ecommerceapp.model.Product;
import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();

    Product getProductById(Long id);

    Product createProduct(Product product);

    Product updateProduct(Long id, Product productDetails);

    void updateProductStock(Long productId, int quantity);

    void deleteProduct(Long id);

    List<Product> getLowStockProducts(int threshold);
}
