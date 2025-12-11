package com.example.learning.controller;

import com.example.learning.entity.ProductEntity;
import com.example.learning.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public List<ProductEntity> getAllProducts(@RequestParam String tenantName) {
        return productService.getAllProducts(tenantName);
    }

    @GetMapping("/{id}")
    public ProductEntity getProductById(
            @RequestParam String tenantName,
            @PathVariable Long id) {
        return productService.getProductById(tenantName, id);
    }

    @PostMapping
    public ProductEntity createProduct(
            @RequestParam String tenantName,
            @RequestBody ProductEntity product) {
        return productService.createProduct(tenantName, product);
    }

    @GetMapping("/category/{categoryId}")
    public List<ProductEntity> getProductsByCategory(
            @RequestParam String tenantName,
            @PathVariable Integer categoryId) {
        return productService.getProductsByCategory(tenantName, categoryId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(
            @RequestParam String tenantName,
            @PathVariable Long id) {
        productService.deleteProduct(tenantName, id);
        return ResponseEntity.ok().build();
    }
}
