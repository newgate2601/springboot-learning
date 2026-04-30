package com.example.learning.catalog.repository;

import com.example.learning.catalog.enums.ProductListingSort;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductListingQueryCriteria {
    private String keyword;
    private List<Long> categoryIds;
    private List<Long> brandIds;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private boolean inStock;
    private ProductListingSort sort;
}
