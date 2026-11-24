package com.mwcm.reconciliation.transaction;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionOutboxDispatch {
    private final TransactionOutboxRepository outbox;
    private final TransactionPublisher publisher;

    public TransactionOutboxDispatch(TransactionOutboxRepository outbox, TransactionPublisher publisher) {
        this.outbox = outbox;
        this.publisher = publisher;
    }

    @Transactional
    public void publish(UUID transactionId) {
        TransactionOutbox entry = outbox.findByTransactionId(transactionId).orElse(null);
        if (entry == null || entry.isPublished()) {
            return;
        }
        publisher.publish(transactionId);
        entry.markPublished();
        outbox.save(entry);
    }
}
