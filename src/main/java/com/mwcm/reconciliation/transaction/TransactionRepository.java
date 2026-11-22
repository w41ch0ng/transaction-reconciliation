package com.mwcm.reconciliation.transaction;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<TransactionRecord, UUID> {
    // JPQL query names the TransactionRecord entity and its fields.
    // Hibernate writes the SELECT for the transactions table. Null parameters skip
    // that filter, and values are bound separately from query text.
    @Query("""
            SELECT t FROM TransactionRecord t
            WHERE (:institutionId IS NULL OR t.institution.id = :institutionId)
              AND (:sourceId IS NULL OR t.source.id = :sourceId)
              AND (:status IS NULL OR t.status = :status)
              AND (:type IS NULL OR t.transactionType = :type)
              AND (:externalReference IS NULL OR t.externalReference = :externalReference)
            """)
    Page<TransactionRecord> search(
            @Param("institutionId") UUID institutionId,
            @Param("sourceId") UUID sourceId,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionType type,
            @Param("externalReference") String externalReference,
            Pageable pageable);
}
