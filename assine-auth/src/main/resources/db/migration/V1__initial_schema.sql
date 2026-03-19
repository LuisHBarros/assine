-- Usuários autenticados
CREATE TABLE auth_users (
    id          UUID PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    provider    VARCHAR(20) NOT NULL,           -- MAGIC_LINK | GOOGLE
    role        VARCHAR(20) NOT NULL DEFAULT 'USER',  -- USER | ADMIN
    last_login  TIMESTAMP
);
CREATE INDEX idx_auth_users_role ON auth_users(role);
CREATE INDEX idx_auth_users_email ON auth_users(email);

-- Tokens de magic link
CREATE TABLE magic_tokens (
    id          UUID PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_magic_tokens_token ON magic_tokens(token);
CREATE INDEX idx_magic_tokens_expires_at ON magic_tokens(expires_at);
