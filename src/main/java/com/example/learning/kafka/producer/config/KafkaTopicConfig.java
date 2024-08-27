package com.example.learning.kafka.producer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic desNewTopic(){
        return TopicBuilder.name("my-topic")
                .build();
    }

    @Bean
    public KafkaAdmin.NewTopics desNewTopics(){
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name("kaka")
                        .build(),
                TopicBuilder.name("kaka1")
                        .build()
        );
    }
}