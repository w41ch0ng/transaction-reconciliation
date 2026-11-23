package com.mwcm.reconciliation.transaction;

import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionProcessor {
    private final TransactionService transactions;

    public TransactionProcessor(TransactionService transactions) {
        this.transactions = transactions;
    }

    @KafkaListener(topics = "${app.kafka.transactions-topic}")
    public void process(String transactionId) {
        transactions.process(UUID.fromString(transactionId));
    }
}
