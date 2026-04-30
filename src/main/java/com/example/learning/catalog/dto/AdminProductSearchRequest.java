package com.example.learning.catalog.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminProductSearchRequest {
    private String keyword;
    private String status;
}
