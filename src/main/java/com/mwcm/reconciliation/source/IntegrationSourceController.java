package com.mwcm.reconciliation.source;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class IntegrationSourceController {
    private final IntegrationSourceService service;

    public IntegrationSourceController(IntegrationSourceService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/institutions/{institutionId}/sources")
    public ResponseEntity<IntegrationSourceResponse> create(
            @PathVariable UUID institutionId,
            @Valid @RequestBody CreateIntegrationSourceRequest request) {
        IntegrationSourceResponse response = service.create(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/institutions/{institutionId}/sources")
    public List<IntegrationSourceResponse> listByInstitution(@PathVariable UUID institutionId) {
        return service.listByInstitution(institutionId);
    }

    @GetMapping("/api/v1/sources/{id}")
    public IntegrationSourceResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }
}
