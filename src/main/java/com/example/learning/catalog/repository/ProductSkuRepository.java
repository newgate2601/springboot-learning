package com.example.learning.catalog.repository;

import com.example.learning.catalog.entity.ProductSkuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSkuRepository extends JpaRepository<ProductSkuEntity, Long> {
}
