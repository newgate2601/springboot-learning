package com.example.learning.service;

import com.example.learning.entity.FactOrderEntity;
import com.example.learning.repository.FactOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FactOrderService {
    private final FactOrderRepository factOrderRepository;

    @Transactional(readOnly = true)
    public Optional<FactOrderEntity> getById(Long id) {
        return factOrderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<FactOrderEntity> getAll(Pageable pageable) {
        return factOrderRepository.findAll(pageable);
    }
}
