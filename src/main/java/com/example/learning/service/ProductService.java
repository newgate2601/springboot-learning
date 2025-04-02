package com.example.learning.service;

import com.example.learning.dto.SearchProductDto;
import com.example.learning.entity.product.AllProductViewEntity;
import com.example.learning.repository.CustomSpecification;
import com.example.learning.repository.product.AllProductViewRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class ProductService {
    private final AllProductViewRepository allProductViewRepository;

    @Transactional(readOnly = true)
    public Page<AllProductViewEntity> getList(SearchProductDto searchProductDto, Pageable pageable) {
        return allProductViewRepository.findAll(getSpe(searchProductDto),
//                pageable
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by(
                                Sort.Order.asc("pcOrder")
                                ,Sort.Order.desc("partCode")
                        )
                )
        );
    }

    private Specification<AllProductViewEntity> getSpe(SearchProductDto searchProductDto) {
        return CustomSpecification.<AllProductViewEntity>builder()
                .search()
                .isLike("productCode", searchProductDto.getProductCode())
                .isLike("partCode", searchProductDto.getProductCode())
                .isLike("po", searchProductDto.getProductCode())
                .build();
    }
}
