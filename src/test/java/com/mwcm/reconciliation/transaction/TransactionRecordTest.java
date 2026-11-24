package com.mwcm.reconciliation.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.mwcm.reconciliation.institution.Institution;
import com.mwcm.reconciliation.source.IntegrationSource;
import com.mwcm.reconciliation.source.SourceType;

class TransactionRecordTest {
    @Test
    void newTransactionStartsPending() {
        TransactionRecord transaction = new TransactionRecord(
                activeSource(),
                "idem-1",
                "TXN-1",
                TransactionType.PAYMENT,
                25000,
                CurrencyCode.GBP,
                Instant.parse("2026-11-13T10:30:00Z"),
                "COMPLETED",
                NormalisedStatus.COMPLETED,
                null);

        assertEquals(TransactionStatus.PENDING, transaction.getStatus());
        assertEquals("idem-1", transaction.getIdempotencyKey());
        assertEquals(25000, transaction.getAmountMinorUnits());
    }

    @Test
    void processingCompletesAPendingTransaction() {
        TransactionRecord transaction = new TransactionRecord(
                activeSource(),
                "idem-1",
                "TXN-1",
                TransactionType.PAYMENT,
                25000,
                CurrencyCode.GBP,
                Instant.parse("2026-11-13T10:30:00Z"),
                "COMPLETED",
                NormalisedStatus.COMPLETED,
                null);

        transaction.beginProcessing();
        assertEquals(TransactionStatus.PROCESSING, transaction.getStatus());
        transaction.completeProcessing();
        assertEquals(TransactionStatus.PROCESSED, transaction.getStatus());
    }

    @Test
    void processingRejectsATransactionThatIsNotPending() {
        TransactionRecord transaction = new TransactionRecord(
                activeSource(),
                "idem-1",
                "TXN-1",
                TransactionType.PAYMENT,
                25000,
                CurrencyCode.GBP,
                Instant.parse("2026-11-13T10:30:00Z"),
                "COMPLETED",
                NormalisedStatus.COMPLETED,
                null);
        transaction.beginProcessing();

        assertThrows(IllegalStateException.class, transaction::beginProcessing);
    }

    @Test
    void failingAPendingTransactionRecordsItAsFailed() {
        TransactionRecord transaction = pendingTransaction();

        transaction.fail();
        assertEquals(TransactionStatus.FAILED, transaction.getStatus());
        transaction.fail();
        assertEquals(TransactionStatus.FAILED, transaction.getStatus());
    }

    @Test
    void failingAProcessedTransactionIsRejected() {
        TransactionRecord transaction = pendingTransaction();
        transaction.beginProcessing();
        transaction.completeProcessing();

        assertThrows(IllegalStateException.class, transaction::fail);
    }

    private TransactionRecord pendingTransaction() {
        return new TransactionRecord(
                activeSource(),
                "idem-1",
                "TXN-1",
                TransactionType.PAYMENT,
                25000,
                CurrencyCode.GBP,
                Instant.parse("2026-11-13T10:30:00Z"),
                "COMPLETED",
                NormalisedStatus.COMPLETED,
                null);
    }

    @Test
    void rejectsBlankExternalReference() {
        assertThrows(IllegalArgumentException.class, () -> new TransactionRecord(
                activeSource(),
                "idem-1",
                " ",
                TransactionType.PAYMENT,
                25000,
                CurrencyCode.GBP,
                Instant.parse("2026-11-13T10:30:00Z"),
                "COMPLETED",
                NormalisedStatus.COMPLETED,
                null));
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> new TransactionRecord(
                activeSource(),
                "idem-1",
                "TXN-1",
                TransactionType.PAYMENT,
                0,
                CurrencyCode.GBP,
                Instant.parse("2026-11-13T10:30:00Z"),
                "COMPLETED",
                NormalisedStatus.COMPLETED,
                null));
    }

    private IntegrationSource activeSource() {
        Institution institution = new Institution("Northstar Bank", "UNIT-BANK");
        return new IntegrationSource("Core ledger", SourceType.INTERNAL_LEDGER, institution);
    }
}
