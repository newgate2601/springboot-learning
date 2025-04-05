package com.example.learning.inboxoutbox.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEntity, String> {
    List<OutboxEntity> findAllByStatus(String status);
}
