package com.mwcm.reconciliation.institution;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import org.springframework.stereotype.Service;

@Service 
public class InstitutionService {
    private final InstitutionRepository repository;
    public InstitutionService(InstitutionRepository repository) {
        this.repository = repository;
    }

    public InstitutionResponse create(CreateInstitutionRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new DuplicateInstitutionCodeException();
        }
        
        Institution saved = repository.save(new Institution(request.name(), request.code()));
        return toResponse(saved);
    }

    public InstitutionResponse getById(UUID id) {
        Institution institution = repository.findById(id).orElseThrow(() -> new InstitutionNotFoundException(id));
        return toResponse(institution);
    }

    public List<InstitutionResponse> list() {
        List<InstitutionResponse> responses = new ArrayList<>();
        for (Institution institution : repository.findAll()) {
            responses.add(toResponse(institution));
        }
        return responses;
    }

    // InstitutionResponse constructor from Institution object
    private InstitutionResponse toResponse(Institution institution) {
        return new InstitutionResponse(
            institution.getId(),
            institution.getName(),
            institution.getCode(),
            institution.isActive(),
            institution.getCreatedAt(),
            institution.getUpdatedAt());
    }
}
