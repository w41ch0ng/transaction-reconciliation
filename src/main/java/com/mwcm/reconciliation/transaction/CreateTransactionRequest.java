package com.mwcm.reconciliation.transaction;

import java.time.Instant;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTransactionRequest(
        @NotBlank @Size(max = 255) String externalReference,
        @NotBlank @Size(max = 32) String transactionType,
        @NotNull @Positive Long amountMinorUnits,
        @NotBlank @Size(max = 3) String currency,
        @NotNull Instant transactionTimestamp,
        @NotBlank @Size(max = 64) String status,
        Map<String, Object> metadata) {}
