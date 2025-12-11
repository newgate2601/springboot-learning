package com.example.learning.service;

import com.example.learning.entity.ProductEntity;
import com.example.learning.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ProductService {
    @Autowired
    private ProductRepository productRepository;


    public List<ProductEntity> getAllProducts(String tenantName) {
        return productRepository.findAll();
    }

    public ProductEntity getProductById(String tenantName, Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public ProductEntity createProduct(String tenantName, ProductEntity product) {
        return productRepository.save(product);
    }

    public List<ProductEntity> getProductsByCategory(String tenantName, Integer categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public void deleteProduct(String tenantName, Long id) {
        productRepository.deleteById(id);
    }
}