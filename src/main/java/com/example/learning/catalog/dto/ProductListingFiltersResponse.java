package com.example.learning.catalog.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductListingFiltersResponse {
    private List<FilterOptionResponse> categories;
    private List<FilterOptionResponse> brands;
}
