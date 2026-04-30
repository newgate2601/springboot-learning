package com.example.learning.catalog.enums;

import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public enum ProductListingSort {
    NEWEST("newest"),
    PRICE_ASC("price_asc"),
    PRICE_DESC("price_desc"),
    NAME_ASC("name_asc"),
    SOLD_DESC("sold_desc");

    private final String requestValue;

    ProductListingSort(String requestValue) {
        this.requestValue = requestValue;
    }

    public static ProductListingSort fromRequestValue(String value) {
        if (value == null || value.isBlank()) {
            return NEWEST;
        }
        for (ProductListingSort sort : values()) {
            if (sort.requestValue.equalsIgnoreCase(value.trim())) {
                return sort;
            }
        }
        throw new ResponseStatusException(BAD_REQUEST, "Sort field không hợp lệ.");
    }
}
