package com.example.learning.inboxoutbox.inbox.handler;

import com.example.learning.inboxoutbox.common.Status;
import com.example.learning.inboxoutbox.inbox.InboxEntity;
import com.example.learning.inboxoutbox.inbox.InboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Slf4j
@AllArgsConstructor
public class InboxConsumerHandler {
    private final InboxRepository inboxRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "my-topic", groupId = "my-group-id", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    @SneakyThrows
    public void listen(String message, Acknowledgment acknowledgment) {
        log.error("Message received: {}", message);
        OutboxDto outboxDto = objectMapper.readValue(message, OutboxDto.class);
        if (inboxRepository.existsById(outboxDto.getId())) {
            log.error("Duplicate message received: {}", message);
        } else {
            inboxRepository.save(
                    InboxEntity.builder()
                            .id(outboxDto.getId())
                            .aggregateId(outboxDto.getAggregateId())
                            .messageType(outboxDto.getMessageType())
                            .payload(outboxDto.getPayload())
                            .status(Status.PENDING.name())
                            .createdAt(outboxDto.getCreatedAt())
                            .build()
            );
            log.info("Persist inbox: {}", message);
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                acknowledgment.acknowledge();
            }
        });
    }
}
