package com.mwcm.reconciliation.transaction;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.mwcm.reconciliation.institution.Institution;
import com.mwcm.reconciliation.source.IntegrationSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "transactions",
        uniqueConstraints = @UniqueConstraint(
                name = "transactions_source_idempotency_key",
                columnNames = {"integration_source_id", "idempotency_key"}))
public class TransactionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "integration_source_id", nullable = false)
    private IntegrationSource source;

    @Column(name = "external_reference", length = 255, nullable = false)
    private String externalReference;

    @Column(name = "idempotency_key", length = 255, nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 32, nullable = false)
    private TransactionType transactionType;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 3, nullable = false)
    private CurrencyCode currency;

    @Column(name = "transaction_timestamp", nullable = false)
    private Instant transactionTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private TransactionStatus status;

    @Column(name = "provider_status", length = 64, nullable = false)
    private String providerStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "normalised_status", length = 32, nullable = false)
    private NormalisedStatus normalisedStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TransactionRecord() {}

    public TransactionRecord(
            IntegrationSource source,
            String idempotencyKey,
            String externalReference,
            TransactionType transactionType,
            long amountMinorUnits,
            CurrencyCode currency,
            Instant transactionTimestamp,
            String providerStatus,
            NormalisedStatus normalisedStatus,
            Map<String, Object> metadata) {
        if (source == null) {
            throw new IllegalArgumentException("source is required");
        }
        if (!source.isActive()) {
            throw new IllegalArgumentException("source must be active");
        }
        if (source.getInstitution() == null || !source.getInstitution().isActive()) {
            throw new IllegalArgumentException("institution is required and must be active");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotency key is required");
        }
        if (idempotencyKey.trim().length() > 255) {
            throw new IllegalArgumentException("idempotency key must be at most 255 characters");
        }
        if (externalReference == null || externalReference.isBlank()) {
            throw new IllegalArgumentException("external reference is required");
        }
        if (transactionType == null) {
            throw new IllegalArgumentException("transaction type is required");
        }
        if (amountMinorUnits <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (currency == null) {
            throw new IllegalArgumentException("currency is required");
        }
        if (transactionTimestamp == null) {
            throw new IllegalArgumentException("transaction timestamp is required");
        }
        if (providerStatus == null || providerStatus.isBlank()) {
            throw new IllegalArgumentException("provider status is required");
        }
        if (normalisedStatus == null) {
            throw new IllegalArgumentException("normalised status is required");
        }

        this.source = source;
        this.institution = source.getInstitution();
        this.idempotencyKey = idempotencyKey.trim();
        this.externalReference = externalReference;
        this.transactionType = transactionType;
        this.amountMinorUnits = amountMinorUnits;
        this.currency = currency;
        this.transactionTimestamp = transactionTimestamp;
        this.status = TransactionStatus.PENDING;
        this.providerStatus = providerStatus.trim();
        this.normalisedStatus = normalisedStatus;
        this.metadata = metadata == null ? null : new HashMap<>(metadata);

        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public Institution getInstitution() {
        return institution;
    }

    public IntegrationSource getSource() {
        return source;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public long getAmountMinorUnits() {
        return amountMinorUnits;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public Instant getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getProviderStatus() {
        return providerStatus;
    }

    public NormalisedStatus getNormalisedStatus() {
        return normalisedStatus;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
