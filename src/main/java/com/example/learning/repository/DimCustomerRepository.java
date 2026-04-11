package com.example.learning.repository;

import com.example.learning.entity.DimCustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DimCustomerRepository extends JpaRepository<DimCustomerEntity, Long> {
    @Query(value = """
            SELECT *
            FROM bd.dim_customer
            WHERE status = :status
            ORDER BY signup_at ASC 
            LIMIT 100
            """, nativeQuery = true)
    List<DimCustomerEntity> findTop100ByStatusOrderBySignupAtAscCustomerIdAsc(@Param("status") Short status);
}
