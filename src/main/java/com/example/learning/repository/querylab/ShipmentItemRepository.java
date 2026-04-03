package com.example.learning.repository.querylab;

import com.example.learning.entity.querylab.ShipmentItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentItemRepository extends JpaRepository<ShipmentItemEntity, Long> {

    List<ShipmentItemEntity> findByShipment_ShipmentNumber(String shipmentNumber);
}
