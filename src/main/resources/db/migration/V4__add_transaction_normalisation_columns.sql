ALTER TABLE transactions
    ADD COLUMN provider_status VARCHAR(64),
    ADD COLUMN normalised_status VARCHAR(32);

UPDATE transactions
SET provider_status = 'COMPLETED',
    normalised_status = 'COMPLETED'
WHERE provider_status IS NULL;

ALTER TABLE transactions
    ALTER COLUMN provider_status SET NOT NULL,
    ALTER COLUMN normalised_status SET NOT NULL;

ALTER TABLE transactions
    ADD CONSTRAINT transactions_normalised_status_check
        CHECK (normalised_status IN ('PENDING', 'COMPLETED', 'FAILED'));
