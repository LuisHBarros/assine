CREATE TABLE access_permissions (
    id               UUID PRIMARY KEY,
    user_id          UUID NOT NULL,
    resource         VARCHAR(100) NOT NULL,   -- "newsletter", futuro: "podcast", etc.
    subscription_id  UUID NOT NULL,           -- referência por ID, sem FK cruzada
    expires_at       TIMESTAMP,              -- null = sem expiração definida
    revoked_at       TIMESTAMP,
    created_at       TIMESTAMP NOT NULL
);

CREATE INDEX idx_access_user_resource ON access_permissions(user_id, resource);