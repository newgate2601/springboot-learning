package com.example.learning.controller;

import com.example.learning.entity.DimProductEntity;
import com.example.learning.service.DimProductService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/dim-product")
@RequiredArgsConstructor
@CrossOrigin
public class DimProductController {
    private final DimProductService dimProductService;

    @GetMapping("/{id}")
    @Operation(summary = "Lay chi tiet dim_product theo id")
    public DimProductEntity getById(@PathVariable Long id) {
        return dimProductService.getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @GetMapping("/list")
    @Operation(summary = "Lay danh sach dim_product theo phan trang")
    public Page<DimProductEntity> getAll(Pageable pageable) {
        return dimProductService.getAll(pageable);
    }
}
