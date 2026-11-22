ALTER TABLE transactions
    ADD COLUMN idempotency_key VARCHAR(255);

UPDATE transactions
SET idempotency_key = id::text
WHERE idempotency_key IS NULL;

ALTER TABLE transactions
    ALTER COLUMN idempotency_key SET NOT NULL;

ALTER TABLE transactions
    ADD CONSTRAINT transactions_source_idempotency_key
        UNIQUE (integration_source_id, idempotency_key);
