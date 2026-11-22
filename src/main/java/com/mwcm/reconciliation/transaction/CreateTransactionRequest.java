package com.mwcm.reconciliation.transaction;

import java.time.Instant;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTransactionRequest(
        @NotBlank @Size(max = 255) String externalReference,
        @NotNull TransactionType transactionType,
        @NotNull @Positive Long amountMinorUnits,
        @NotNull CurrencyCode currency,
        @NotNull Instant transactionTimestamp,
        Map<String, Object> metadata) {}
