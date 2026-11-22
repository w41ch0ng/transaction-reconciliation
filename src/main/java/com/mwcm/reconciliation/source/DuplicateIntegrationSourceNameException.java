package com.mwcm.reconciliation.source;

public class DuplicateIntegrationSourceNameException extends RuntimeException {
    public DuplicateIntegrationSourceNameException() {
        super("name already in use");
    }
}
