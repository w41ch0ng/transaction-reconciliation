CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions (id),
    integration_source_id UUID NOT NULL REFERENCES integration_sources (id),
    external_reference VARCHAR(255) NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    amount_minor_units BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    transaction_timestamp TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT transactions_type_check
        CHECK (transaction_type IN ('PAYMENT', 'REFUND', 'REVERSAL')),
    CONSTRAINT transactions_status_check
        CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED')),
    CONSTRAINT transactions_currency_check
        CHECK (currency IN ('GBP', 'USD', 'EUR')),
    CONSTRAINT transactions_amount_check
        CHECK (amount_minor_units > 0)
);

CREATE INDEX transactions_institution_id_idx ON transactions (institution_id);
CREATE INDEX transactions_integration_source_id_idx ON transactions (integration_source_id);
CREATE INDEX transactions_external_reference_idx ON transactions (external_reference);
