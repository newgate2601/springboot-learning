package com.example.learning.repository.querylab;

import com.example.learning.entity.querylab.ProductSupplierMapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSupplierMapRepository extends JpaRepository<ProductSupplierMapEntity, Long> {

    List<ProductSupplierMapEntity> findByProduct_Id(Long productId);

    List<ProductSupplierMapEntity> findBySupplier_Id(Long supplierId);
}
