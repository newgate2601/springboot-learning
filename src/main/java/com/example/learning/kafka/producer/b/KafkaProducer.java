//package com.example.learning.kafka.producer.b;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//
//@Service
//public class KafkaProducer {
//
//    @Autowired
//    private KafkaTemplate<String, String> kafkaTemplate;
//
//    public void sendMessage(String message){
//        System.out.println("Send message: " + message + " to Topic: " + AppConstants.TOPIC_NAME);
//        kafkaTemplate.send(AppConstants.TOPIC_NAME, message);
//    }
//}
