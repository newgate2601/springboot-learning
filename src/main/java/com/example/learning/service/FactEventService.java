package com.example.learning.service;

import com.example.learning.entity.FactEventEntity;
import com.example.learning.repository.FactEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FactEventService {
    private final FactEventRepository factEventRepository;

    @Transactional(readOnly = true)
    public Optional<FactEventEntity> getById(Long id) {
        return factEventRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<FactEventEntity> getAll(Pageable pageable) {
        return factEventRepository.findAll(pageable);
    }
}
