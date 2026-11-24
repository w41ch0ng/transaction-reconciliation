package com.mwcm.reconciliation.transaction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class TransactionOutboxTest {
    @Test
    void newEntryIsUnpublishedUntilItIsMarked() {
        TransactionOutbox outbox = new TransactionOutbox(UUID.randomUUID());

        assertFalse(outbox.isPublished());
        outbox.markPublished();
        assertTrue(outbox.isPublished());
        outbox.markPublished();
        assertTrue(outbox.isPublished());
    }

    @Test
    void rejectsAMissingTransactionId() {
        assertThrows(IllegalArgumentException.class, () -> new TransactionOutbox(null));
    }
}
