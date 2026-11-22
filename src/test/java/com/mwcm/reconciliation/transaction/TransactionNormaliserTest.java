package com.mwcm.reconciliation.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TransactionNormaliserTest {
    private final TransactionNormaliser normaliser = new TransactionNormaliser();

    @Test
    void mapsProviderTypesOntoInternalTypes() {
        assertEquals(TransactionType.PAYMENT, normaliser.normaliseType("SALE"));
        assertEquals(TransactionType.PAYMENT, normaliser.normaliseType("purchase"));
        assertEquals(TransactionType.REFUND, normaliser.normaliseType("CREDIT"));
        assertEquals(TransactionType.REVERSAL, normaliser.normaliseType(" reverse "));
    }

    @Test
    void mapsProviderStatusesOntoOneVocabulary() {
        assertEquals(NormalisedStatus.COMPLETED, normaliser.normaliseStatus("COMPLETED"));
        assertEquals(NormalisedStatus.COMPLETED, normaliser.normaliseStatus("settled"));
        assertEquals(NormalisedStatus.FAILED, normaliser.normaliseStatus("DECLINED"));
        assertEquals(NormalisedStatus.PENDING, normaliser.normaliseStatus("AUTHORISED"));
        assertEquals(NormalisedStatus.PENDING, normaliser.normaliseStatus("authorized"));
    }

    @Test
    void acceptsCurrencyInEitherCase() {
        assertEquals(CurrencyCode.GBP, normaliser.normaliseCurrency("gbp"));
        assertEquals(CurrencyCode.USD, normaliser.normaliseCurrency("USD"));
    }

    @Test
    void rejectsUnknownValues() {
        assertThrows(UnrecognisedTransactionValueException.class, () -> normaliser.normaliseType("WIRE"));
        assertThrows(UnrecognisedTransactionValueException.class, () -> normaliser.normaliseStatus("HOLD"));
        assertThrows(UnrecognisedTransactionValueException.class, () -> normaliser.normaliseCurrency("XYZ"));
    }
}
