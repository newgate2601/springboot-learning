package com.example.learning.repository;

import com.example.learning.entity.DimLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DimLocationRepository extends JpaRepository<DimLocationEntity, Long> {
}
