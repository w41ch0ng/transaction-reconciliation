package com.mwcm.reconciliation.transaction;

public class UnrecognisedTransactionValueException extends RuntimeException {
    public UnrecognisedTransactionValueException(String field, String value) {
        super("unrecognised " + field + ": " + value);
    }
}
