package com.example.learning.kafka.producer.producer_result_proxy;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.ProducerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProducerResultProxy<K, V> implements ProducerListener<K, V> {
    @Override
    public void onSuccess(ProducerRecord<K, V> producerRecord, RecordMetadata recordMetadata) {
        System.out.println("Oke");
        ProducerListener.super.onSuccess(producerRecord, recordMetadata);
    }

    @Override
    public void onError(ProducerRecord<K, V> producerRecord, RecordMetadata recordMetadata, Exception exception) {
        System.out.println("Fail");
        ProducerListener.super.onError(producerRecord, recordMetadata, exception);
    }
}
