package com.mwcm.reconciliation.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;

import com.mwcm.reconciliation.institution.Institution;
import com.mwcm.reconciliation.institution.InstitutionRepository;
import com.mwcm.reconciliation.source.IntegrationSource;
import com.mwcm.reconciliation.source.IntegrationSourceRepository;
import com.mwcm.reconciliation.source.SourceType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest {
    private final TransactionRepository repository;
    private final InstitutionRepository institutionRepository;
    private final IntegrationSourceRepository sourceRepository;

    @Autowired
    TransactionRepositoryTest(
            TransactionRepository repository,
            InstitutionRepository institutionRepository,
            IntegrationSourceRepository sourceRepository) {
        this.repository = repository;
        this.institutionRepository = institutionRepository;
        this.sourceRepository = sourceRepository;
    }

    @Test
    void savesAndFindsByExternalReference() {
        IntegrationSource source = savedSource("Tx Repo Bank", "TX-REPO", "Core ledger");
        repository.save(transaction(source, "TXN-REPO-1"));

        var page = repository.search(
                null,
                null,
                null,
                null,
                "TXN-REPO-1",
                PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
        TransactionRecord found = page.getContent().get(0);
        assertEquals("TXN-REPO-1", found.getExternalReference());
        assertEquals(TransactionStatus.PENDING, found.getStatus());
        assertEquals(25000, found.getAmountMinorUnits());
        assertNotNull(found.getId());
    }

    @Test
    void searchFiltersByInstitution() {
        IntegrationSource first = savedSource("First Tx Bank", "TX-REPO-1", "Ledger");
        IntegrationSource second = savedSource("Second Tx Bank", "TX-REPO-2", "Ledger");
        repository.save(transaction(first, "TXN-A"));
        repository.save(transaction(second, "TXN-B"));

        var page = repository.search(
                first.getInstitution().getId(),
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
        assertEquals("TXN-A", page.getContent().get(0).getExternalReference());
    }

    @Test
    void idempotencyKeyIsUniqueForEachSource() {
        IntegrationSource source = savedSource("Idem Bank", "TX-REPO-IDEM", "Ledger");
        IntegrationSource other = savedSource("Idem Bank Two", "TX-REPO-IDEM-2", "Ledger");
        repository.saveAndFlush(transaction(source, "TXN-1", "same-key"));
        repository.saveAndFlush(transaction(other, "TXN-2", "same-key"));

        TransactionRecord found = repository.findBySourceIdAndIdempotencyKey(source.getId(), "same-key")
                .orElseThrow();
        assertEquals("TXN-1", found.getExternalReference());

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(transaction(source, "TXN-3", "same-key")));
    }

    private IntegrationSource savedSource(String institutionName, String code, String sourceName) {
        Institution institution = institutionRepository.save(new Institution(institutionName, code));
        return sourceRepository.save(
                new IntegrationSource(sourceName, SourceType.INTERNAL_LEDGER, institution));
    }

    private TransactionRecord transaction(IntegrationSource source, String externalReference) {
        return transaction(source, externalReference, externalReference);
    }

    private TransactionRecord transaction(
            IntegrationSource source,
            String externalReference,
            String idempotencyKey) {
        return new TransactionRecord(
                source,
                idempotencyKey,
                externalReference,
                TransactionType.PAYMENT,
                25000,
                CurrencyCode.GBP,
                Instant.parse("2026-11-13T10:30:00Z"),
                "COMPLETED",
                NormalisedStatus.COMPLETED,
                null);
    }
}
