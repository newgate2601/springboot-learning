package com.example.learning.inboxoutbox.outbox.handler;

import com.example.learning.inboxoutbox.common.Status;
import com.example.learning.inboxoutbox.outbox.OutboxEntity;
import com.example.learning.inboxoutbox.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class OutboxServiceImpl implements OutboxService {
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public void persistOutbox(String aggregateId,
                              String messageType,
                              String destination,
                              Object payload) {
        String payloadJson = objectMapper.writeValueAsString(payload);
        String outboxId = UUID.randomUUID().toString();
        outboxRepository.save(
                OutboxEntity.builder()
                        .id(outboxId)
                        .aggregateId(aggregateId)
                        .messageType(messageType)
                        .payload(payloadJson)
                        .status(Status.PENDING.name())
                        .destination(destination)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        log.debug("Persist outbox success with payload: {} with id: {}", payloadJson, outboxId);
    }
}
