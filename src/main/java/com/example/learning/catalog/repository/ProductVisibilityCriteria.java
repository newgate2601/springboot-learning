package com.example.learning.catalog.repository;

import com.example.learning.catalog.enums.BrandStatus;
import com.example.learning.catalog.enums.CategoryStatus;
import com.example.learning.catalog.enums.ProductStatus;
import com.example.learning.catalog.enums.SkuStatus;

import java.math.BigDecimal;

public record ProductVisibilityCriteria(
        ProductStatus activeProductStatus,
        CategoryStatus activeCategoryStatus,
        BrandStatus activeBrandStatus,
        SkuStatus activeSkuStatus,
        BigDecimal minSalePrice
) {
    public static ProductVisibilityCriteria defaultVisible() {
        return new ProductVisibilityCriteria(
                ProductStatus.ACTIVE,
                CategoryStatus.ACTIVE,
                BrandStatus.ACTIVE,
                SkuStatus.ACTIVE,
                BigDecimal.ZERO
        );
    }
}
