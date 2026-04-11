package com.example.learning.repository;

import com.example.learning.entity.FactOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FactOrderRepository extends JpaRepository<FactOrderEntity, Long> {
}
