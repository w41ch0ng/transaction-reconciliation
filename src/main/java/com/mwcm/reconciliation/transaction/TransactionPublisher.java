package com.mwcm.reconciliation.transaction;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionPublisher {
    private final KafkaTemplate<String, String> kafka;
    private final String topic;
    private final String deadLetterTopic;

    public TransactionPublisher(
            KafkaTemplate<String, String> kafka,
            @Value("${app.kafka.transactions-topic}") String topic,
            @Value("${app.kafka.transactions-dead-letter-topic}") String deadLetterTopic) {
        this.kafka = kafka;
        this.topic = topic;
        this.deadLetterTopic = deadLetterTopic;
    }

    public void publish(UUID transactionId) {
        String id = transactionId.toString();
        kafka.send(topic, id, id);
    }

    public void publishFailed(UUID transactionId) {
        String id = transactionId.toString();
        try {
            kafka.send(deadLetterTopic, id, id).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("failed transaction could not be recorded", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("failed transaction could not be recorded", exception);
        }
    }
}
