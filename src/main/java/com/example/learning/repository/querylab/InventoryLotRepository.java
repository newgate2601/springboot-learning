package com.example.learning.repository.querylab;

import com.example.learning.entity.querylab.InventoryLotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryLotRepository extends JpaRepository<InventoryLotEntity, Long> {

    List<InventoryLotEntity> findByWarehouse_WarehouseCodeAndProduct_Sku(String warehouseCode, String sku);
}
