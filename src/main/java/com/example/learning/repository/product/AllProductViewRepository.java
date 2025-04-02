package com.example.learning.repository.product;

import com.example.learning.entity.product.AllProductViewEntity;
import com.example.learning.entity.product.ProductMappingEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AllProductViewRepository extends JpaRepository<AllProductViewEntity, Long>,
        JpaSpecificationExecutor<AllProductViewEntity> {
}
