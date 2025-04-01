package com.example.learning.controller;

import com.example.learning.entity.product.AllProductViewEntity;
import com.example.learning.service.ProductService;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/product")
@AllArgsConstructor
@CrossOrigin
public class ProductController {
    private final ProductService productService;

    @GetMapping("/list")
    public Page<AllProductViewEntity> getList(@ParameterObject Pageable pageable) {
        return productService.getList(pageable);
    }
}
