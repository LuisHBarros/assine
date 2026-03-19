CREATE TABLE plans (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    price_cents INTEGER NOT NULL,
    interval    VARCHAR(20) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL
);

CREATE TABLE subscriptions (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL,
    plan_id             UUID NOT NULL REFERENCES plans(id),
    payment_method      VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    current_period_end  TIMESTAMP,
    canceled_at         TIMESTAMP,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_status  ON subscriptions(status);

-- Seed basic plans
INSERT INTO plans (id, name, price_cents, interval, active, created_at)
VALUES 
('018e519e-9d2c-7000-8000-000000000001', 'Mensal', 2990, 'MONTHLY', true, NOW()),
('018e519e-9d2c-7000-8000-000000000002', 'Anual', 29900, 'ANNUAL', true, NOW());
