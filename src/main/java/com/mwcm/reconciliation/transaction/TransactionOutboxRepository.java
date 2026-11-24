package com.mwcm.reconciliation.transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionOutboxRepository extends JpaRepository<TransactionOutbox, UUID> {
    Optional<TransactionOutbox> findByTransactionId(UUID transactionId);

    List<TransactionOutbox> findByPublishedAtIsNullOrderByCreatedAtAsc();
}
