package com.mwcm.reconciliation.institution;

public class DuplicateInstitutionCodeException extends RuntimeException {
    public DuplicateInstitutionCodeException() {
        super("code already in use");
    }
}
