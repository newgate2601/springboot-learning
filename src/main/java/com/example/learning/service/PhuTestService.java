package com.example.learning.service;

import com.example.learning.repository.PhuTestRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PhuTestService {
    private final PhuTestRepository phuTestRepository;

    // Create or Update Product
    public Product saveProduct(Phu product) {
        return productRepository.save(product);
    }

    // Get all Products
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Get Product by ID
    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    // Delete Product by ID
    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }
}
