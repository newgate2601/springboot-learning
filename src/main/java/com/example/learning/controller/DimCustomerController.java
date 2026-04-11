package com.example.learning.controller;

import com.example.learning.entity.DimCustomerEntity;
import com.example.learning.service.DimCustomerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dim-customer")
@RequiredArgsConstructor
@CrossOrigin
public class DimCustomerController {
    private final DimCustomerService dimCustomerService;

    @GetMapping("/{id}")
    @Operation(summary = "Lay chi tiet dim_customer theo id")
    public DimCustomerEntity getById(@PathVariable Long id) {
        return dimCustomerService.getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }

    @GetMapping("/list")
    @Operation(summary = "Lay danh sach dim_customer theo phan trang")
    public Page<DimCustomerEntity> getAll(Pageable pageable) {
        return dimCustomerService.getAll(pageable);
    }

    @GetMapping("/active-earliest")
    @Operation(summary = "Lay 100 khach hang active dang ky som nhat")
    public List<DimCustomerEntity> getEarliestActiveCustomers() {
        return dimCustomerService.getEarliestActiveCustomers();
    }
}
