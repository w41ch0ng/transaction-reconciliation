package com.mwcm.reconciliation.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TransactionAcceptedListener {
    private static final Logger log = LoggerFactory.getLogger(TransactionAcceptedListener.class);

    private final TransactionOutboxDispatch dispatch;

    public TransactionAcceptedListener(TransactionOutboxDispatch dispatch) {
        this.dispatch = dispatch;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccepted(TransactionAccepted event) {
        try {
            dispatch.publish(event.transactionId());
        } catch (RuntimeException exception) {
            log.warn("transaction {} could not be published: {}", event.transactionId(), exception.getMessage());
        }
    }
}
