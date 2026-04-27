package com.example.learning.catalog.dto;

import lombok.*;
import org.springframework.data.domain.Page;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeHighlightsResponse {
    private Page<HomeBannerResponse> banners;
    private Page<HomeCategoryResponse> featuredCategories;
    private Page<HomeProductCardResponse> featuredProducts;
    private Page<HomeProductCardResponse> newProducts;
    private Page<HomeProductCardResponse> bestSellingProducts;
}
