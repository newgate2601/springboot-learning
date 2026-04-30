package com.example.learning.catalog.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptionResponse {
    private Long id;
    private String name;
    private String slug;
}
