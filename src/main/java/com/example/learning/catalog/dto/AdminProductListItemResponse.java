package com.example.learning.catalog.dto;

import com.example.learning.catalog.enums.ProductStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductListItemResponse {
    private Long productId;
    private String name;
    private String slug;
    private String mainImageUrl;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private ProductStatus status;
    private Long skuCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
