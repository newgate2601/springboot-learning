package com.example.learning.repository.querylab;

import com.example.learning.entity.querylab.PurchaseOrderLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLineEntity, Long> {

    List<PurchaseOrderLineEntity> findByPurchaseOrder_PoNumber(String poNumber);
}
