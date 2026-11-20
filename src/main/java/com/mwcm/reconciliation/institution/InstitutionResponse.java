package com.mwcm.reconciliation.institution;

import java.time.Instant;
import java.util.UUID;

public record InstitutionResponse(UUID id, String name, String code, boolean active, Instant createdAt, Instant updatedAt) {
    
}
