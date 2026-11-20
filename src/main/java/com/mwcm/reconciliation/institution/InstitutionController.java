package com.mwcm.reconciliation.institution;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;



@RestController
@RequestMapping("/api/v1/institutions")
public class InstitutionController {
    private final InstitutionService service;
    public InstitutionController(InstitutionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<InstitutionResponse> create(@Valid @RequestBody CreateInstitutionRequest request) {
        InstitutionResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    public InstitutionResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @GetMapping
    public List<InstitutionResponse> list() {
        return service.list();
    }
}
