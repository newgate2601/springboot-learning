package com.example.learning.service;

import com.example.learning.entity.DimCustomerEntity;
import com.example.learning.repository.DimCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DimCustomerService {
    private static final short ACTIVE_STATUS = 1;

    private final DimCustomerRepository dimCustomerRepository;

    @Transactional(readOnly = true)
    public Optional<DimCustomerEntity> getById(Long id) {
        return dimCustomerRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<DimCustomerEntity> getAll(Pageable pageable) {
        return dimCustomerRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<DimCustomerEntity> getEarliestActiveCustomers() {
        return dimCustomerRepository.findTop100ByStatusOrderBySignupAtAscCustomerIdAsc(ACTIVE_STATUS);
    }
}
