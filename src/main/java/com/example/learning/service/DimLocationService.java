package com.example.learning.service;

import com.example.learning.entity.DimLocationEntity;
import com.example.learning.repository.DimLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DimLocationService {
    private final DimLocationRepository dimLocationRepository;

    @Transactional(readOnly = true)
    public Optional<DimLocationEntity> getById(Long id) {
        return dimLocationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<DimLocationEntity> getAll(Pageable pageable) {
        return dimLocationRepository.findAll(pageable);
    }
}
