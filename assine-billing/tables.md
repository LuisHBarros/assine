-- Pagamentos processados
CREATE TABLE payments (
    id               UUID PRIMARY KEY,
    subscription_id  UUID NOT NULL,           -- referência por ID, sem FK cruzada
    external_id      VARCHAR(255) NOT NULL,   -- ID do Stripe
    idempotency_key  VARCHAR(255) NOT NULL UNIQUE,
    amount_cents     INTEGER NOT NULL,
    payment_method   VARCHAR(20) NOT NULL,    -- CREDIT_CARD | PIX
    status           VARCHAR(20) NOT NULL,    -- PENDING|CONFIRMED|FAILED
    confirmed_at     TIMESTAMP,
    created_at       TIMESTAMP NOT NULL
);

-- Garante que o mesmo webhook não seja processado duas vezes
CREATE TABLE processed_webhook_events (
    external_event_id  VARCHAR(255) PRIMARY KEY,  -- ID do evento do Stripe
    processed_at       TIMESTAMP NOT NULL
);

-- Outbox Pattern: eventos a publicar na fila
CREATE TABLE outbox_events (
    id            UUID PRIMARY KEY,
    aggregate_id  UUID NOT NULL,              -- ID do Payment
    event_type    VARCHAR(100) NOT NULL,      -- "PaymentConfirmed"
    payload       JSONB NOT NULL,
    published     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL
);

CREATE INDEX idx_outbox_unpublished ON outbox_events(published) WHERE published = FALSE;

-- Chargebacks (contestações de pagamento)
CREATE TABLE chargebacks (
    id              UUID PRIMARY KEY,
    payment_id      UUID NOT NULL REFERENCES payments(id),
    subscription_id UUID NOT NULL,
    external_id     VARCHAR(255) NOT NULL,     -- ID da disputa no Stripe
    amount_cents    INTEGER NOT NULL,
    status          VARCHAR(20) NOT NULL,     -- OPENED | WON | LOST
    opened_at       TIMESTAMP NOT NULL,
    resolved_at     TIMESTAMP
);

-- Reembolsos
CREATE TABLE refunds (
    id                     UUID PRIMARY KEY,
    payment_id             UUID NOT NULL REFERENCES payments(id),
    subscription_id        UUID NOT NULL,
    external_id            VARCHAR(255),                -- ID do reembolso no Stripe
    requested_at           TIMESTAMP NOT NULL,
    activated_at           DATE NOT NULL,
    days_since_activation  INTEGER NOT NULL,
    refund_percentage       NUMERIC(5,2) NOT NULL,
    original_amount_cents  INTEGER NOT NULL,
    refund_amount_cents    INTEGER NOT NULL,
    status                 VARCHAR(20) NOT NULL,        -- PENDING | COMPLETED | FAILED
    reason                 VARCHAR(255),
    completed_at           TIMESTAMP
);
```

O fluxo atômico do webhook fica assim numa transação só:
```
BEGIN
  INSERT INTO processed_webhook_events (...)   -- idempotência
  INSERT INTO payments (...)                   -- persiste o pagamento
  INSERT INTO outbox_events (published=false)  -- agenda publicação
COMMIT

-- depois, o OutboxPublisher em polling:
  SELECT * FROM outbox_events WHERE published = false
  → publica no RabbitMQ
  → UPDATE outbox_events SET published = true
```
