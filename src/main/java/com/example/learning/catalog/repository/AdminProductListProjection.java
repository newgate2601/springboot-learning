package com.example.learning.catalog.repository;

import java.time.LocalDateTime;

public interface AdminProductListProjection {
    Long getProductId();

    String getProductName();

    String getSlug();

    String[] getImageUrls();

    Long getCategoryId();

    String getCategoryName();

    Long getBrandId();

    String getBrandName();

    String getStatus();

    Long getSkuCount();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();

    LocalDateTime getPublishedAt();
}
