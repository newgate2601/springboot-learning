package com.example.learning.catalog.repository;

import com.example.learning.catalog.entity.BrandEntity;
import com.example.learning.catalog.enums.BrandStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BrandRepository extends JpaRepository<BrandEntity, Long> {
    List<BrandEntity> findByStatusOrderByNameAsc(BrandStatus status);

    List<BrandEntity> findByIdInAndStatus(Collection<Long> ids, BrandStatus status);
}
