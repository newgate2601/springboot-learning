package com.example.learning.controller;

import com.example.learning.entity.DimLocationEntity;
import com.example.learning.service.DimLocationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/dim-location")
@RequiredArgsConstructor
@CrossOrigin
public class DimLocationController {
    private final DimLocationService dimLocationService;

    @GetMapping("/{id}")
    @Operation(summary = "Lay chi tiet dim_location theo id")
    public DimLocationEntity getById(@PathVariable Long id) {
        return dimLocationService.getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
    }

    @GetMapping("/list")
    @Operation(summary = "Lay danh sach dim_location theo phan trang")
    public Page<DimLocationEntity> getAll(Pageable pageable) {
        return dimLocationService.getAll(pageable);
    }
}
