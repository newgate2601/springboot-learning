package com.example.learning.repository.product;

import com.example.learning.entity.product.ProductPartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductPartRepository extends JpaRepository<ProductPartEntity, Long> {
}
