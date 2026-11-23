package com.mwcm.reconciliation.transaction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TransactionAcceptedListener {
    private final TransactionPublisher publisher;

    public TransactionAcceptedListener(TransactionPublisher publisher) {
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccepted(TransactionAccepted event) {
        publisher.publish(event.transactionId());
    }
}
