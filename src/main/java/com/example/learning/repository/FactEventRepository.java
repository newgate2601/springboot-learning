package com.example.learning.repository;

import com.example.learning.entity.FactEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FactEventRepository extends JpaRepository<FactEventEntity, Long> {
}
