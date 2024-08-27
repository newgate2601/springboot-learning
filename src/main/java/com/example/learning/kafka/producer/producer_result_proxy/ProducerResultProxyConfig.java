package com.example.learning.kafka.producer.producer_result_proxy;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@AllArgsConstructor
@Configuration
public class ProducerResultProxyConfig {
    @Bean
    public ProducerResultProxy<String, String> producerListener() {
        return new ProducerResultProxy<>();
    }
}
