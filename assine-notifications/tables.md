CREATE TABLE notification_history (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL,
    template       VARCHAR(100) NOT NULL,
    channel        VARCHAR(20) NOT NULL,
    status         VARCHAR(20) NOT NULL,   -- SENT | FAILED
    correlation_id VARCHAR(255),
    sent_at        TIMESTAMP,
    created_at     TIMESTAMP NOT NULL
);