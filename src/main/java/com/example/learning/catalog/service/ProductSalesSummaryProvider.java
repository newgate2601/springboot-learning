package com.example.learning.catalog.service;

import com.example.learning.catalog.dto.ProductSalesSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductSalesSummaryProvider {
    Page<ProductSalesSummary> getBestSellingProductSummaries(Pageable pageable);
}
