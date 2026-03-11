-- Usuários autenticados
CREATE TABLE auth_users (
    id          UUID PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    provider    VARCHAR(20) NOT NULL,           -- MAGIC_LINK | GOOGLE
    role        VARCHAR(20) NOT NULL DEFAULT 'USER',  -- USER | ADMIN
    created_at  TIMESTAMP NOT NULL,
    last_login  TIMESTAMP
);

CREATE INDEX idx_auth_users_email ON auth_users(email);
CREATE INDEX idx_auth_users_role ON auth_users(role);

-- Tokens de magic link
CREATE TABLE magic_tokens (
    id          UUID PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_magic_tokens_token ON magic_tokens(token);
CREATE INDEX idx_magic_tokens_expires_at ON magic_tokens(expires_at);

Job de limpeza diário (via scheduler):
```
DELETE FROM magic_tokens
WHERE expires_at < NOW()
AND used = TRUE
```

Account linking (busca usuário por email):
```
SELECT * FROM auth_users WHERE email = ?
→ se existe: autentica usuário existente (ignora provider original)
→ se não existe: cria novo AuthUser com provider atual
```

Magic link validation (token de uso único):
```
SELECT * FROM magic_tokens WHERE token = ?
→ se não existe ou expirou ou used = TRUE: retorna 401
→ se válido:
  1. UPDATE magic_tokens SET used = TRUE WHERE token = ?
  2. aplica account linking pelo email
  3. emite JWT
```
