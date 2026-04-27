package com.example.learning.catalog.service;

import com.example.learning.catalog.dto.ProductStockSummary;

import java.util.List;

public interface ProductStockProvider {
    List<ProductStockSummary> getStockSummaries(List<Long> productIds);
}
