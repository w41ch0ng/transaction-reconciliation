package com.mwcm.reconciliation.transaction;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(UUID id) {
        super("transaction not found: " + id);
    }
}
