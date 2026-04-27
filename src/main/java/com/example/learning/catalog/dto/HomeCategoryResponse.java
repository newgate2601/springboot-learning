package com.example.learning.catalog.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeCategoryResponse {
    private Long categoryId;
    private String name;
    private String slug;
}
