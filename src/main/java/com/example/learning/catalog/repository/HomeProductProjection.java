package com.example.learning.catalog.repository;

import java.math.BigDecimal;

public interface HomeProductProjection {
    Long getProductId();

    String getProductName();

    String getSlug();

    String getShortDescription();

    String getBrandName();

    String getCategoryName();

    String[] getImageUrls();

    BigDecimal getMinSalePrice();

    BigDecimal getMinComparePrice();
}
