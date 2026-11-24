package com.mwcm.reconciliation.transaction;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TransactionOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(TransactionOutboxPublisher.class);

    private final TransactionOutboxRepository outbox;
    private final TransactionOutboxDispatch dispatch;

    public TransactionOutboxPublisher(TransactionOutboxRepository outbox, TransactionOutboxDispatch dispatch) {
        this.outbox = outbox;
        this.dispatch = dispatch;
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox-retry-interval-ms}")
    public void publishPending() {
        for (TransactionOutbox entry : outbox.findByPublishedAtIsNullOrderByCreatedAtAsc()) {
            UUID transactionId = entry.getTransactionId();
            try {
                dispatch.publish(transactionId);
            } catch (RuntimeException exception) {
                log.warn("transaction {} could not be published: {}", transactionId, exception.getMessage());
            }
        }
    }
}
