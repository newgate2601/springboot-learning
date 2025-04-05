package com.example.learning.inboxoutbox.inbox.handler;

import com.example.learning.inboxoutbox.common.Status;
import com.example.learning.inboxoutbox.inbox.InboxEntity;
import com.example.learning.inboxoutbox.inbox.InboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@AllArgsConstructor
public class InboxConsumerHandler {
    private final InboxRepository inboxRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "my-topic", groupId = "my-group-id")
//    @Transactional
//    @SneakyThrows
    public void listen(String message) throws JsonProcessingException {
        log.error("Message received: {}", message);
        OutboxDto outboxDto = objectMapper.readValue(message, OutboxDto.class);
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
    }
}
