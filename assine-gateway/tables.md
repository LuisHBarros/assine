CREATE TABLE notification_history (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL,
    template       VARCHAR(100) NOT NULL,     -- "welcome", "payment_confirmed", etc.
    channel        VARCHAR(20) NOT NULL,      -- EMAIL | SMS
    status         VARCHAR(20) NOT NULL,      -- SENT | FAILED
    correlation_id VARCHAR(255),             -- rastreabilidade
    sent_at        TIMESTAMP,
    created_at     TIMESTAMP NOT NULL
);