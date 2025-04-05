package com.example.learning.controller;

import com.example.learning.kafka.producer.KafkaProducer;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("kafka")
@AllArgsConstructor
public class KafkaController {
    private final KafkaProducer kafkaProducer;

    @PostMapping("/syn-send-message")
    public void synSendMessage(@RequestParam String topic) throws InterruptedException {
        String mes = "abc";
        for (int i = 1; i <= 5; i++) {
            kafkaProducer.sendMessage(topic, mes + i);
            Thread.sleep(3000); // Thêm delay nhỏ
        }
    }

    @PostMapping("/asyn-send-message")
    public void asynSendMessage(@RequestParam String topic) {
        String mes = "abc";
        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> {
                    kafkaProducer.sendMessage(topic, mes + "1");
                }),
                CompletableFuture.runAsync(() -> {
                    kafkaProducer.sendMessage(topic, mes + "2");
                }),
                CompletableFuture.runAsync(() -> {
                    kafkaProducer.sendMessage(topic, mes + "3");
                }),
                CompletableFuture.runAsync(() -> {
                    kafkaProducer.sendMessage(topic, mes + "4");
                }),
                CompletableFuture.runAsync(() -> {
                    kafkaProducer.sendMessage(topic, mes + "5");
                })
        );
    }
}
