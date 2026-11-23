package com.mwcm.reconciliation.transaction;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionPublisher {
    private final KafkaTemplate<String, String> kafka;
    private final String topic;

    public TransactionPublisher(
            KafkaTemplate<String, String> kafka,
            @Value("${app.kafka.transactions-topic}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    public void publish(UUID transactionId) {
        String id = transactionId.toString();
        kafka.send(topic, id, id);
    }
}
