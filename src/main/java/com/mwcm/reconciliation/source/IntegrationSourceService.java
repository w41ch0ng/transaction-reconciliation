package com.mwcm.reconciliation.source;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mwcm.reconciliation.institution.Institution;
import com.mwcm.reconciliation.institution.InstitutionNotFoundException;
import com.mwcm.reconciliation.institution.InstitutionRepository;

@Service
@Transactional
public class IntegrationSourceService {
    private final IntegrationSourceRepository repository;
    private final InstitutionRepository institutionRepository;

    public IntegrationSourceService(
            IntegrationSourceRepository repository,
            InstitutionRepository institutionRepository) {
        this.repository = repository;
        this.institutionRepository = institutionRepository;
    }

    public IntegrationSourceResponse create(UUID institutionId, CreateIntegrationSourceRequest request) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new InstitutionNotFoundException(institutionId));

        if (repository.existsByInstitutionIdAndName(institutionId, request.name())) {
            throw new DuplicateIntegrationSourceNameException();
        }

        IntegrationSource saved = repository.save(
                new IntegrationSource(request.name(), request.sourceType(), institution));
        return toResponse(saved);
    }

    public List<IntegrationSourceResponse> listByInstitution(UUID institutionId) {
        institutionRepository.findById(institutionId)
                .orElseThrow(() -> new InstitutionNotFoundException(institutionId));

        List<IntegrationSourceResponse> responses = new ArrayList<>();
        for (IntegrationSource source : repository.findByInstitutionId(institutionId)) {
            responses.add(toResponse(source));
        }
        return responses;
    }

    public IntegrationSourceResponse getById(UUID id) {
        IntegrationSource source = repository.findById(id)
                .orElseThrow(() -> new IntegrationSourceNotFoundException(id));
        return toResponse(source);
    }

    private IntegrationSourceResponse toResponse(IntegrationSource source) {
        return new IntegrationSourceResponse(
                source.getId(),
                source.getInstitution().getId(),
                source.getName(),
                source.getSourceType(),
                source.isActive(),
                source.getCreatedAt(),
                source.getUpdatedAt());
    }
}
