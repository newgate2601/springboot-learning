package com.example.learning.repository;

import com.example.learning.entity.DimProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DimProductRepository extends JpaRepository<DimProductEntity, Long> {
}
