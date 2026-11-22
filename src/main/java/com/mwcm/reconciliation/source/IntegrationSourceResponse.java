package com.mwcm.reconciliation.source;

import java.time.Instant;
import java.util.UUID;

public record IntegrationSourceResponse (UUID id, UUID institutionId, String name, SourceType sourceType, boolean active, Instant createdAt, Instant updatedAt) {}
