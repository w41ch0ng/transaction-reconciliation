package com.mwcm.reconciliation.transaction;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transaction_outbox")
public class TransactionOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private UUID transactionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected TransactionOutbox() {}

    public TransactionOutbox(UUID transactionId) {
        if (transactionId == null) {
            throw new IllegalArgumentException("transaction id is required");
        }
        this.transactionId = transactionId;
        this.createdAt = Instant.now();
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public boolean isPublished() {
        return publishedAt != null;
    }

    public void markPublished() {
        if (this.publishedAt == null) {
            this.publishedAt = Instant.now();
        }
    }
}
