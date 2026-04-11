package com.example.learning.controller;

import com.example.learning.entity.FactEventEntity;
import com.example.learning.service.FactEventService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/fact-event")
@RequiredArgsConstructor
@CrossOrigin
public class FactEventController {
    private final FactEventService factEventService;

    @GetMapping("/{id}")
    @Operation(summary = "Lay chi tiet fact_events theo id")
    public FactEventEntity getById(@PathVariable Long id) {
        return factEventService.getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    @GetMapping("/list")
    @Operation(summary = "Lay danh sach fact_events theo phan trang")
    public Page<FactEventEntity> getAll(Pageable pageable) {
        return factEventService.getAll(pageable);
    }
}
