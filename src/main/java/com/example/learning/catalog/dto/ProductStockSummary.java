package com.example.learning.catalog.dto;

import com.example.learning.catalog.enums.StockDisplayStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockSummary {
    private Long productId;
    private StockDisplayStatus stockStatus;
}
