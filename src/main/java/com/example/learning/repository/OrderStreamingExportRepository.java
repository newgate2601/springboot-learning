package com.example.learning.repository;

import com.example.learning.dto.OrderExportRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
public class OrderStreamingExportRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<OrderExportRow> findNextBatch(Long lastIdExclusive, int batchSize) {
        List<OrderExportRow> rows = entityManager.createQuery("""
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
                        where (:lastId is null or o.id > :lastId)
                        order by o.id
                        """, OrderExportRow.class)
                .setParameter("lastId", lastIdExclusive)
                .setMaxResults(batchSize)
                .getResultList();

        entityManager.clear();
        return rows;
    }

    @Transactional(readOnly = true)
    public long countOrders() {
        Long total = entityManager.createQuery("""
                        select count(o.id)
                        from OrderEntity o
                        """, Long.class)
                .getSingleResult();
        return total == null ? 0L : total;
    }
}
