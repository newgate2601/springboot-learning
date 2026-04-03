package com.example.learning.repository.querylab;

import com.example.learning.entity.querylab.SalesOrderLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderLineRepository extends JpaRepository<SalesOrderLineEntity, Long> {

    List<SalesOrderLineEntity> findBySalesOrder_SoNumber(String soNumber);
}
