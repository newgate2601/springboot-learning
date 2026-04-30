package com.example.learning.catalog.repository;

import com.example.learning.catalog.enums.BrandStatus;
import com.example.learning.catalog.enums.CategoryStatus;
import com.example.learning.catalog.enums.ProductStatus;
import com.example.learning.catalog.enums.SkuStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ProductVisibilityCriteria {
    private ProductStatus activeProductStatus;
    private CategoryStatus activeCategoryStatus;
    private BrandStatus activeBrandStatus;
    private SkuStatus activeSkuStatus;
    private BigDecimal minSalePrice;

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
