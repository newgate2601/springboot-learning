package com.example.learning.repository.querylab;

import com.example.learning.entity.querylab.ShipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<ShipmentEntity, Long> {

    Optional<ShipmentEntity> findByShipmentNumber(String shipmentNumber);
}
