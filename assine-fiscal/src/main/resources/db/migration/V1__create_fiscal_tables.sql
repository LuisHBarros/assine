-- Notas fiscais emitidas
CREATE TABLE invoices (
    id                UUID PRIMARY KEY,
    payment_id        UUID NOT NULL UNIQUE,
    subscription_id   UUID NOT NULL,
    user_id           UUID NOT NULL,
    external_id       VARCHAR(255),                  -- ID da nota na Nuvem Fiscal
    series            VARCHAR(10),
    number            VARCHAR(20),
    amount_cents      INTEGER NOT NULL,
    status            VARCHAR(20) NOT NULL,           -- PENDING|ISSUED|FAILED|CANCELED
    issuer_response   JSONB,
    pdf_url           VARCHAR(500),                -- URL interna do MinIO (http://minio:9000/invoices/{id}.pdf)
    issued_at         TIMESTAMP,
    created_at        TIMESTAMP NOT NULL
);

CREATE INDEX idx_invoices_payment_id ON invoices(payment_id);
CREATE INDEX idx_invoices_status ON invoices(status);

-- Outbox de emissão para resiliência
CREATE TABLE invoice_outbox (
    id                UUID PRIMARY KEY,
    payment_id        UUID NOT NULL UNIQUE,
    payload           JSONB NOT NULL,
    attempts          INTEGER NOT NULL DEFAULT 0,
    last_attempt_at   TIMESTAMP,
    issued            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP NOT NULL
);

CREATE INDEX idx_invoice_outbox_unpublished ON invoice_outbox(issued) WHERE issued = FALSE;
