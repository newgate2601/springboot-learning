package com.example.learning.repository.querylab;

import com.example.learning.entity.querylab.BusinessUnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessUnitRepository extends JpaRepository<BusinessUnitEntity, Long> {

    Optional<BusinessUnitEntity> findByUnitCode(String unitCode);
}
