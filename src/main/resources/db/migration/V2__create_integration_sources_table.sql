CREATE TABLE integration_sources (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions (id),
    name VARCHAR(255) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT integration_sources_type_check
        CHECK (source_type IN ('INTERNAL_LEDGER', 'PAYMENT_PROCESSOR')),
    CONSTRAINT integration_sources_institution_name_key
        UNIQUE (institution_id, name)
)