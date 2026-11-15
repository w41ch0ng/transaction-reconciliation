package com.mwcm.reconciliation.institution;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<Institution, UUID> {
    Optional<Institution> findByCode(String code);
    boolean existsByCode(String code);
}