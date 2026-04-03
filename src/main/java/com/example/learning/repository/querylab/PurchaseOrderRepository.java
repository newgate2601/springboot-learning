package com.example.learning.repository.querylab;

import com.example.learning.entity.querylab.PurchaseOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, Long> {

    Optional<PurchaseOrderEntity> findByPoNumber(String poNumber);

    List<PurchaseOrderEntity> findBySupplier_IdAndOrderDateBetween(Long supplierId, LocalDate fromDate, LocalDate toDate);
}
