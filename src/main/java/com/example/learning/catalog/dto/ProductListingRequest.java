package com.example.learning.catalog.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductListingRequest {
    private String keyword;
    private Long categoryId;
    private String categorySlug;
    private List<Long> brandIds;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean inStock;
    private String sort;
}
