package com.example.learning.catalog.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSalesSummary {
    private Long productId;
    private Long soldCount;
}
