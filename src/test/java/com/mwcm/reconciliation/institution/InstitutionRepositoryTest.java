package com.mwcm.reconciliation.institution;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InstitutionRepositoryTest {
	private final InstitutionRepository repository;

	@Autowired
	InstitutionRepositoryTest(InstitutionRepository repository) {
		this.repository = repository;
	}

	@Test
	void findByCodeReturnsSavedInstitution() {
		repository.save(new Institution("Northstar Bank", "REPO-FIND"));

		Institution found = repository.findByCode("REPO-FIND").orElseThrow();

		assertEquals("REPO-FIND", found.getCode());
		assertNotNull(found.getId());
	}

	@Test
	void existsByCodeMatchesSavedCodeOnly() {
		repository.save(new Institution("Southsun Bank", "SOUTHSUN"));

		assertTrue(repository.existsByCode("SOUTHSUN"));
		assertFalse(repository.existsByCode("MISSING"));
	}

	@Test
	void rejectsDuplicateCode() {
		repository.saveAndFlush(new Institution("First Bank", "DUPLICATE"));
		Institution duplicate = new Institution("Second Bank", "DUPLICATE");

		assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicate));
	}
}