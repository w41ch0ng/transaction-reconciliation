package com.mwcm.reconciliation.source;

import java.util.UUID;

public class IntegrationSourceNotFoundException extends RuntimeException {
    public IntegrationSourceNotFoundException(UUID id) {
        super("integration source not found: " + id);
    }
}
