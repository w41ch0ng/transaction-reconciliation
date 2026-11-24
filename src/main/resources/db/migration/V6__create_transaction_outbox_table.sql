CREATE TABLE transaction_outbox (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL UNIQUE REFERENCES transactions (id),
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX transaction_outbox_unpublished_idx
    ON transaction_outbox (created_at)
    WHERE published_at IS NULL;
