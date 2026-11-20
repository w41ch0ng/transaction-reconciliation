package com.mwcm.reconciliation.institution;

import java.util.UUID;

public class InstitutionNotFoundException extends RuntimeException {
    public InstitutionNotFoundException(UUID id) {
        super("institution not found: " + id);
    }
}
