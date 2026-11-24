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
        send(topic, transactionId, "transaction could not be published");
    }

    public void publishFailed(UUID transactionId) {
        send(deadLetterTopic, transactionId, "failed transaction could not be recorded");
    }

    private void send(String destination, UUID transactionId, String failureMessage) {
        String id = transactionId.toString();
        try {
            kafka.send(destination, id, id).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(failureMessage, exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException(failureMessage, exception);
        }
    }
}
