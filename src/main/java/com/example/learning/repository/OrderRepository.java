package com.example.learning.repository;

import com.example.learning.dto.OrderExportRow;
import com.example.learning.entity.OrderEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByOrderNo(String orderNo);

    @Query("""
            select new com.example.learning.dto.OrderExportRow(
                o.id,
                o.orderNo,
                o.customerId,
                o.status,
                o.totalAmount,
                o.orderDate,
                o.createdAt,
                o.updatedAt
            )
            from OrderEntity o
            order by o.id
            """)
    List<OrderExportRow> findOrderExportRows(Pageable pageable);
}
