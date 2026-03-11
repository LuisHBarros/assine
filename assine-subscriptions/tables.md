-- Planos disponíveis
CREATE TABLE plans (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,        -- "Mensal", "Anual"
    price_cents INTEGER NOT NULL,             -- centavos, nunca float
    interval    VARCHAR(20) NOT NULL,         -- MONTHLY | ANNUAL
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL
);

-- Agregado raiz do domínio
CREATE TABLE subscriptions (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL,
    plan_id             UUID NOT NULL REFERENCES plans(id),
    payment_method      VARCHAR(20) NOT NULL,    -- CREDIT_CARD | PIX
    status              VARCHAR(20) NOT NULL,    -- PENDING|ACTIVE|PAST_DUE|CANCELED
    current_period_end  TIMESTAMP,
    canceled_at         TIMESTAMP,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_status  ON subscriptions(status);
