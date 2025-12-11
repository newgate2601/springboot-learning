package com.example.learning.controller;

import com.example.learning.service.TenantService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@AllArgsConstructor
public class TenantController {
    private final TenantService tenantService;

    @PostMapping
    public void createTenant(@RequestParam String tenantName) {
        tenantService.createTenant(tenantName);
    }
}
