package com.example.learning.service;

import com.example.learning.entity.product.AllProductViewEntity;
import com.example.learning.repository.product.AllProductViewRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class ProductService {
    private final AllProductViewRepository allProductViewRepository;

    @Transactional(readOnly = true)
    public Page<AllProductViewEntity> getList(Pageable pageable) {
        return allProductViewRepository.findAll(pageable);
    }
}
