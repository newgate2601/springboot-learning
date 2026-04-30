package com.example.learning.catalog.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ProductListingProductProjection {
    Long getProductId();

    String getProductName();

    String getSlug();

    String getShortDescription();

    String getBrandName();

    String getCategoryName();

    String[] getImageUrls();

    BigDecimal getMinSalePrice();

    BigDecimal getMinComparePrice();

    LocalDateTime getPublishedAt();
}
