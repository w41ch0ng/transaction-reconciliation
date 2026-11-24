package com.mwcm.reconciliation.transaction;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.stereotype.Component;

@Component
public class TransactionProcessingRecoverer implements ConsumerRecordRecoverer {
    private final TransactionService transactions;
    private final TransactionPublisher publisher;

    public TransactionProcessingRecoverer(TransactionService transactions, TransactionPublisher publisher) {
        this.transactions = transactions;
        this.publisher = publisher;
    }

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception exception) {
        UUID transactionId = UUID.fromString(String.valueOf(record.value()));
        transactions.fail(transactionId);
        publisher.publishFailed(transactionId);
    }
}
