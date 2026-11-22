package com.mwcm.reconciliation.source;

import java.util.UUID;

import com.mwcm.reconciliation.institution.Institution;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;

@Entity
@Table(
    name = "integration_sources",
    uniqueConstraints = @UniqueConstraint(
        name = "integration_sources_institution_name_key",
        columnNames = {"institution_id", "name"}
    ))
public class IntegrationSource {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;
    @Column(name = "name", length = 255, nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 32, nullable = false)
    private SourceType sourceType;
    @Column(name = "active", nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // empty constructor for hibernate
    protected IntegrationSource() {}

    // public constructor for application code to call
    public IntegrationSource(String name, SourceType sourceType, Institution institution) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        } else {
            throw new IllegalArgumentException("name is required");
        }
        
        if (sourceType != null) {
            this.sourceType = sourceType;
        } else {
            throw new IllegalArgumentException("sourceType is required");
        }

        if (institution != null && institution.isActive()) {
            this.institution = institution;
        } else {
            throw new IllegalArgumentException("institution is required and must be active");
        }

        this.active = true;

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

    public String getName() {
        return name;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}