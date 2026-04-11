package com.example.learning.service;

import com.example.learning.entity.DimProductEntity;
import com.example.learning.repository.DimProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DimProductService {
    private final DimProductRepository dimProductRepository;

    @Transactional(readOnly = true)
    public Optional<DimProductEntity> getById(Long id) {
        return dimProductRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<DimProductEntity> getAll(Pageable pageable) {
        return dimProductRepository.findAll(pageable);
    }
}
