package com.mwcm.reconciliation.institution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInstitutionRequest(
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Size(max = 40) String code) {}