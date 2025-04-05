package com.example.learning.kafka;

import lombok.AllArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@AllArgsConstructor
public class KafkaConfig {
    private final ProducerFactory<String, String> producerFactory;

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic myTopic() {
        return new NewTopic("my-topic", // topic name
                3, // partition numbers
                (short) 1); // replica factor
    }
}
