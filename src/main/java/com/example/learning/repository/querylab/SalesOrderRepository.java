package com.example.learning.repository.querylab;

import com.example.learning.entity.querylab.SalesOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrderEntity, Long> {

    Optional<SalesOrderEntity> findBySoNumber(String soNumber);

    List<SalesOrderEntity> findByCustomer_IdAndOrderDateBetween(Long customerId, LocalDate fromDate, LocalDate toDate);
}
