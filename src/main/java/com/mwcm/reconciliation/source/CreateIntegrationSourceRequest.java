package com.mwcm.reconciliation.source;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIntegrationSourceRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull SourceType sourceType) {}
