package com.mwcm.reconciliation.transaction;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
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
    private static final String DIFFERENT_REQUEST = "idempotency key already used with a different request";
    private static final String ALREADY_IN_USE = "idempotency key already in use";

    private final TransactionRepository repository;
    private final IntegrationSourceRepository sourceRepository;
    private final TransactionNormaliser normaliser;
    private final IdempotencyStore idempotencyStore;
    private final TransactionOutboxRepository outbox;
    private final ApplicationEventPublisher events;

    public TransactionService(
            TransactionRepository repository,
            IntegrationSourceRepository sourceRepository,
            TransactionNormaliser normaliser,
            IdempotencyStore idempotencyStore,
            TransactionOutboxRepository outbox,
            ApplicationEventPublisher events) {
        this.repository = repository;
        this.sourceRepository = sourceRepository;
        this.normaliser = normaliser;
        this.idempotencyStore = idempotencyStore;
        this.outbox = outbox;
        this.events = events;
    }

    public TransactionResponse create(UUID sourceId, String idempotencyKey, CreateTransactionRequest request) {
        String key = validatedKey(idempotencyKey);
        IntegrationSource source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new IntegrationSourceNotFoundException(sourceId));

        Optional<TransactionRecord> existing = findExisting(sourceId, key);
        if (existing.isPresent()) {
            return replay(existing.get(), request);
        }

        try {
            TransactionRecord saved = repository.saveAndFlush(new TransactionRecord(
                    source,
                    key,
                    request.externalReference(),
                    normaliser.normaliseType(request.transactionType()),
                    request.amountMinorUnits(),
                    normaliser.normaliseCurrency(request.currency()),
                    request.transactionTimestamp(),
                    normaliser.providerStatus(request.status()),
                    normaliser.normaliseStatus(request.status()),
                    request.metadata()));
            outbox.save(new TransactionOutbox(saved.getId()));
            idempotencyStore.remember(sourceId, key, saved.getId());
            events.publishEvent(new TransactionAccepted(saved.getId()));
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isIdempotencyConflict(exception)) {
                throw new IdempotencyKeyConflictException(ALREADY_IN_USE);
            }
            throw exception;
        }
    }

    public void process(UUID id) {
        TransactionRecord transaction = repository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        if (transaction.getStatus() == TransactionStatus.PROCESSED
                || transaction.getStatus() == TransactionStatus.FAILED) {
            return;
        }
        transaction.beginProcessing();
        transaction.completeProcessing();
        repository.save(transaction);
    }

    public void fail(UUID id) {
        Optional<TransactionRecord> found = repository.findById(id);
        if (found.isEmpty()) {
            return;
        }
        TransactionRecord transaction = found.get();
        if (transaction.getStatus() == TransactionStatus.PROCESSED
                || transaction.getStatus() == TransactionStatus.FAILED) {
            return;
        }
        transaction.fail();
        repository.save(transaction);
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

    private String validatedKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotency key is required");
        }
        String key = idempotencyKey.trim();
        if (key.length() > 255) {
            throw new IllegalArgumentException("idempotency key must be at most 255 characters");
        }
        return key;
    }

    private Optional<TransactionRecord> findExisting(UUID sourceId, String idempotencyKey) {
        Optional<UUID> cachedId = idempotencyStore.find(sourceId, idempotencyKey);
        if (cachedId.isPresent()) {
            Optional<TransactionRecord> cached = repository.findById(cachedId.get());
            if (cached.isPresent()) {
                return cached;
            }
        }
        Optional<TransactionRecord> stored = repository.findBySourceIdAndIdempotencyKey(sourceId, idempotencyKey);
        stored.ifPresent(transaction -> idempotencyStore.remember(sourceId, idempotencyKey, transaction.getId()));
        return stored;
    }

    private TransactionResponse replay(TransactionRecord existing, CreateTransactionRequest request) {
        if (!sameSubmission(existing, request)) {
            throw new IdempotencyKeyConflictException(DIFFERENT_REQUEST);
        }
        idempotencyStore.remember(existing.getSource().getId(), existing.getIdempotencyKey(), existing.getId());
        return toResponse(existing);
    }

    private boolean sameSubmission(TransactionRecord existing, CreateTransactionRequest request) {
        return existing.getExternalReference().equals(request.externalReference())
                && existing.getTransactionType() == normaliser.normaliseType(request.transactionType())
                && existing.getAmountMinorUnits() == request.amountMinorUnits()
                && existing.getCurrency() == normaliser.normaliseCurrency(request.currency())
                && existing.getTransactionTimestamp().equals(request.transactionTimestamp())
                && existing.getProviderStatus().equals(normaliser.providerStatus(request.status()))
                && existing.getNormalisedStatus() == normaliser.normaliseStatus(request.status())
                && Objects.equals(existing.getMetadata(), request.metadata());
    }

    private boolean isIdempotencyConflict(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current.getMessage() != null
                    && current.getMessage().contains("transactions_source_idempotency_key")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private TransactionResponse toResponse(TransactionRecord transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getInstitution().getId(),
                transaction.getSource().getId(),
                transaction.getIdempotencyKey(),
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
