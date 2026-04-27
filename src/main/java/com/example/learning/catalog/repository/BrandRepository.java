package com.example.learning.catalog.repository;

import com.example.learning.catalog.entity.BrandEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<BrandEntity, Long> {
}
