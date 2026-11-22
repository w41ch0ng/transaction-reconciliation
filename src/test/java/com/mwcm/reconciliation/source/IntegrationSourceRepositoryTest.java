package com.mwcm.reconciliation.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import com.mwcm.reconciliation.institution.Institution;
import com.mwcm.reconciliation.institution.InstitutionRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IntegrationSourceRepositoryTest {
    private final IntegrationSourceRepository repository;
    private final InstitutionRepository institutionRepository;

	@Autowired
	IntegrationSourceRepositoryTest(
        IntegrationSourceRepository repository,
        InstitutionRepository institutionRepository) {
		this.repository = repository;
        this.institutionRepository = institutionRepository;
	}

    @Test
    void savesAndReturnsIntegrationSource() {
		Institution institution = institutionRepository.save(
            new Institution("Northstar Bank", "SOURCE-BANK"));
        
        repository.save(
            new IntegrationSource("Core ledger", SourceType.INTERNAL_LEDGER, institution));

        List<IntegrationSource> sources = repository.findByInstitutionId(institution.getId());
        
        assertEquals(1, sources.size());
        IntegrationSource found = sources.get(0);
        assertEquals("Core ledger", found.getName());
        assertEquals(SourceType.INTERNAL_LEDGER, found.getSourceType());
        assertNotNull(found.getId());
    }

    @Test
    void existsByInstitutionIdAndName() {
        Institution institution = institutionRepository.save(
            new Institution("Northstar Bank", "SOURCE-EXISTS"));
        
        IntegrationSource source = repository.save(
            new IntegrationSource("Core ledger", SourceType.INTERNAL_LEDGER, institution));
        
        assertTrue(repository.existsByInstitutionIdAndName(
            institution.getId(), source.getName()));
        assertFalse(repository.existsByInstitutionIdAndName(
            institution.getId(), "NOT-SAVED"));
    }

    @Test
    void rejectsDuplicateNameForTheSameInstitution() {
        Institution first = institutionRepository.save(
                new Institution("First Bank", "SOURCE-ONE"));
        Institution second = institutionRepository.save(
                new Institution("Second Bank", "SOURCE-TWO"));

        repository.saveAndFlush(
                new IntegrationSource("Core ledger", SourceType.INTERNAL_LEDGER, first));
        repository.saveAndFlush(
                new IntegrationSource("Core ledger", SourceType.PAYMENT_PROCESSOR, second));

        IntegrationSource duplicate = new IntegrationSource(
                "Core ledger", SourceType.PAYMENT_PROCESSOR, first);

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(duplicate));
    }
}
