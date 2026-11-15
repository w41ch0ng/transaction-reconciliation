package com.mwcm.reconciliation.institution;

import java.util.UUID;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "institutions")
public class Institution {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "name", length = 255, nullable = false)
    private String name;
    @Column(name = "code", length = 40, nullable = false, unique = true)
    private String code;
    @Column(name = "active", nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // empty constructor for hibernate
    protected Institution() {}

    // public constructor for application code to call
    public Institution(String name, String code) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        } else {
            throw new IllegalArgumentException("name is required");
        }
        
        if (code != null && !code.isBlank()) {
            this.code = code;
        } else {
            throw new IllegalArgumentException("code is required");
        }

        this.active = true;

        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
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