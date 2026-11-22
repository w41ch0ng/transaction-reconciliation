package com.mwcm.reconciliation.transaction;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mwcm.reconciliation.source.IntegrationSource;
import com.mwcm.reconciliation.source.IntegrationSourceNotFoundException;
import com.mwcm.reconciliation.source.IntegrationSourceRepository;

@Service
@Transactional
public class TransactionService {
    private final TransactionRepository repository;
    private final IntegrationSourceRepository sourceRepository;
    private final TransactionNormaliser normaliser;

    public TransactionService(
            TransactionRepository repository,
            IntegrationSourceRepository sourceRepository,
            TransactionNormaliser normaliser) {
        this.repository = repository;
        this.sourceRepository = sourceRepository;
        this.normaliser = normaliser;
    }

    public TransactionResponse create(UUID sourceId, CreateTransactionRequest request) {
        IntegrationSource source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new IntegrationSourceNotFoundException(sourceId));

        TransactionRecord saved = repository.save(new TransactionRecord(
                source,
                request.externalReference(),
                normaliser.normaliseType(request.transactionType()),
                request.amountMinorUnits(),
                normaliser.normaliseCurrency(request.currency()),
                request.transactionTimestamp(),
                normaliser.providerStatus(request.status()),
                normaliser.normaliseStatus(request.status()),
                request.metadata()));
        return toResponse(saved);
    }

    public TransactionResponse getById(UUID id) {
        TransactionRecord transaction = repository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        return toResponse(transaction);
    }

    public Page<TransactionResponse> list(
            UUID institutionId,
            UUID sourceId,
            TransactionStatus status,
            TransactionType type,
            String externalReference,
            Pageable pageable) {
        String reference = externalReference == null || externalReference.isBlank()
                ? null
                : externalReference;
        return repository.search(institutionId, sourceId, status, type, reference, pageable)
                .map(this::toResponse);
    }

    private TransactionResponse toResponse(TransactionRecord transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getInstitution().getId(),
                transaction.getSource().getId(),
                transaction.getExternalReference(),
                transaction.getTransactionType(),
                transaction.getAmountMinorUnits(),
                transaction.getCurrency(),
                transaction.getTransactionTimestamp(),
                transaction.getStatus(),
                transaction.getProviderStatus(),
                transaction.getNormalisedStatus(),
                transaction.getMetadata(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt());
    }
}
