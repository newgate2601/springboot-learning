package com.example.learning.controller;

import com.example.learning.entity.FactOrderEntity;
import com.example.learning.service.FactOrderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/fact-order")
@RequiredArgsConstructor
@CrossOrigin
public class FactOrderController {
    private final FactOrderService factOrderService;

    @GetMapping("/{id}")
    @Operation(summary = "Lay chi tiet fact_orders theo id")
    public FactOrderEntity getById(@PathVariable Long id) {
        return factOrderService.getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    @GetMapping("/list")
    @Operation(summary = "Lay danh sach fact_orders theo phan trang")
    public Page<FactOrderEntity> getAll(Pageable pageable) {
        return factOrderService.getAll(pageable);
    }
}
