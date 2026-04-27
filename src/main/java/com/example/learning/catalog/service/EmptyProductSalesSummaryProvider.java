package com.example.learning.catalog.service;

import com.example.learning.catalog.dto.ProductSalesSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class EmptyProductSalesSummaryProvider implements ProductSalesSummaryProvider {
    @Override
    public Page<ProductSalesSummary> getBestSellingProductSummaries(Pageable pageable) {
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }
}
