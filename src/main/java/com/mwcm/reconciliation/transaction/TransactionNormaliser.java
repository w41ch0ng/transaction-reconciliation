package com.mwcm.reconciliation.transaction;

import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class TransactionNormaliser {
    public TransactionType normaliseType(String raw) {
        return switch (canonical(raw, "transaction type")) {
            case "PAYMENT", "SALE", "PURCHASE" -> TransactionType.PAYMENT;
            case "REFUND", "CREDIT" -> TransactionType.REFUND;
            case "REVERSAL", "REVERSE" -> TransactionType.REVERSAL;
            default -> reject("transaction type", raw);
        };
    }

    public NormalisedStatus normaliseStatus(String raw) {
        return switch (canonical(raw, "status")) {
            case "PENDING", "AUTHORISED", "AUTHORIZED" -> NormalisedStatus.PENDING;
            case "COMPLETED", "SETTLED" -> NormalisedStatus.COMPLETED;
            case "FAILED", "DECLINED", "REJECTED" -> NormalisedStatus.FAILED;
            default -> reject("status", raw);
        };
    }

    public CurrencyCode normaliseCurrency(String raw) {
        return switch (canonical(raw, "currency")) {
            case "GBP" -> CurrencyCode.GBP;
            case "USD" -> CurrencyCode.USD;
            case "EUR" -> CurrencyCode.EUR;
            default -> reject("currency", raw);
        };
    }

    public String providerStatus(String raw) {
        canonical(raw, "status");
        return raw.trim();
    }

    private String canonical(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new UnrecognisedTransactionValueException(field, raw);
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private <T> T reject(String field, String raw) {
        throw new UnrecognisedTransactionValueException(field, raw);
    }
}
