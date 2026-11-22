package com.mwcm.reconciliation.source;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationSourceRepository extends JpaRepository<IntegrationSource, UUID> {
    List<IntegrationSource> findByInstitutionId(UUID id);
    boolean existsByInstitutionIdAndName(UUID institutionId, String name);
}
