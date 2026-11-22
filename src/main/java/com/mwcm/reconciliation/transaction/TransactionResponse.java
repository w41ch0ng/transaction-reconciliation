package com.mwcm.reconciliation.transaction;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID institutionId,
        UUID sourceId,
        String externalReference,
        TransactionType transactionType,
        long amountMinorUnits,
        CurrencyCode currency,
        Instant transactionTimestamp,
        TransactionStatus status,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt) {}
