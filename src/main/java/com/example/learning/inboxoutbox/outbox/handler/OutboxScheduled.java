package com.example.learning.inboxoutbox.outbox.handler;

import com.example.learning.inboxoutbox.common.Status;
import com.example.learning.inboxoutbox.outbox.OutboxEntity;
import com.example.learning.inboxoutbox.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@AllArgsConstructor
public class OutboxScheduled {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedRate = 5000)
    @Transactional
    @SneakyThrows
    public void outboxScheduled() {
//        log.error("Outbox scheduled handle started !!!");
        List<OutboxEntity> outboxEntities = outboxRepository.findAllByStatus(Status.PENDING.name());
        if (Objects.nonNull(outboxEntities) && !outboxEntities.isEmpty()) {
            outboxEntities = outboxEntities.stream().sorted(Comparator.comparing(OutboxEntity::getCreatedAt).reversed())
                    .toList();
            for (OutboxEntity outboxEntity : outboxEntities) {
                kafkaTemplate.send(outboxEntity.getDestination(),
                        objectMapper.writeValueAsString(outboxEntity)
                        );
                outboxEntity.setStatus(Status.PROCESSED.name());
                outboxRepository.save(outboxEntity);
            }
//            throw new RuntimeException("server fail !!!");
//            log.error("Outbox scheduled handle success !!!");
        } else {
//            log.error("Outbox empty !!!");
        }
    }
}
