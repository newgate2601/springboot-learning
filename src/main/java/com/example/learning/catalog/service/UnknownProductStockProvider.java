package com.example.learning.catalog.service;

import com.example.learning.catalog.dto.ProductStockSummary;
import com.example.learning.catalog.enums.StockDisplayStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UnknownProductStockProvider implements ProductStockProvider {
    @Override
    public List<ProductStockSummary> getStockSummaries(List<Long> productIds) {
        return productIds.stream()
                .map(productId -> ProductStockSummary.builder()
                        .productId(productId)
                        .stockStatus(StockDisplayStatus.UNKNOWN)
                        .build())
                .collect(Collectors.toList());
    }
}
