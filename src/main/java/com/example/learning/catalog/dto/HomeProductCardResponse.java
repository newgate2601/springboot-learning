package com.example.learning.catalog.dto;

import com.example.learning.catalog.enums.StockDisplayStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeProductCardResponse {
    private Long productId;
    private String name;
    private String slug;
    private String shortDescription;
    private String brandName;
    private String categoryName;
    private String mainImageUrl;
    private BigDecimal minSalePrice;
    private BigDecimal minComparePrice;
    private Long soldCount;
    private StockDisplayStatus stockStatus;
}
