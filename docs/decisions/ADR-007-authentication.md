# ADR-007: Autenticação via Magic Link e Google OAuth2 com Account Linking

## Status
Aceito

## Contexto
O sistema Assine precisa autenticar usuários de forma segura e sem fricção. O perfil do usuário é simples: visualiza cobranças, cancela assinatura e solicita reembolso. Não há justificativa para impor gerenciamento de senha a esse perfil. Adicionalmente, o sistema deve garantir que um mesmo email nunca resulte em duas contas distintas, independente do método de autenticação utilizado.

As opções consideradas foram:
1. Usuário e senha com hash (Argon2)
2. Magic link por email
3. Google OAuth2
4. Magic link + Google OAuth2 com account linking

## Decisão
Suportar magic link e Google OAuth2 simultaneamente, com account linking baseado em email. O email é a identidade canônica do usuário — não o provider. Um usuário que se cadastrou via magic link e posteriormente tenta autenticar via Google é reconhecido como o mesmo usuário, desde que o email seja idêntico.

## Account Linking

A tabela auth_users possui restrição UNIQUE em email. Quando um usuário autentica por qualquer provider, o fluxo é sempre:

1. Extrair o email do provider (magic link: email informado; Google: email retornado pelo Google API)
2. Buscar AuthUser pelo email na tabela auth_users
3. Se não existe: criar novo AuthUser com o provider atual
4. Se existe: autenticar o usuário existente independente do provider original — o provider não é validado no login, apenas o email

Isso significa que um usuário cadastrado via magic link que autentica via Google será logado na mesma conta. O campo provider na tabela registra o provider do cadastro original e não é atualizado em logins subsequentes por providers diferentes.

## Fluxo do Magic Link

Etapa 1 — Solicitação:
POST /auth/magic-link { email }
- assine-auth gera token UUID criptograficamente seguro
- Persiste MagicToken com expiresAt = agora + 15 minutos
- Publica MagicLinkRequested para assine-notifications
- assine-notifications envia email com link: https://assine.com/auth/validate?token={token}
- Resposta sempre 200 — não revelar se o email existe ou não

Etapa 2 — Validação:
GET /auth/magic-link/validate?token={token}
- Busca MagicToken pelo token
- Valida: existe, não expirou (expiresAt > now()), used = false
- Marca used = true (uso único — reuso retorna 401)
- Aplica account linking pelo email
- Emite JWT (accessToken + refreshToken)

Etapa 3 — Token de uso único:
O token é invalidado imediatamente após o primeiro uso. Qualquer tentativa de reutilizar o mesmo token retorna 401 com mensagem "Token inválido ou já utilizado".

Etapa 4 — Expiração:
Tokens não utilizados expiram em 15 minutos. Um job de limpeza diário remove tokens expirados da tabela magic_tokens.

## Fluxo do Google OAuth2

Etapa 1 — Redirecionamento:
GET /auth/oauth2/google
- assine-auth redireciona para o endpoint de autorização do Google com os scopes: openid, email, profile

Etapa 2 — Callback:
GET /auth/oauth2/google/callback?code={code}
- assine-auth troca o code por access token via Google Token API
- Busca perfil do usuário (email, nome) via Google UserInfo API
- Aplica account linking pelo email
- Emite JWT (accessToken + refreshToken)

Etapa 3 — Account linking:
Mesmo mecanismo do magic link — o email retornado pelo Google é usado para buscar ou criar o AuthUser.

## JWT emitido

Algoritmo: RS256
- Chave privada: armazenada no assine-auth, usada para assinar
- Chave pública: exposta via GET /auth/.well-known/jwks.json, usada pelo gateway para validar localmente sem chamada HTTP

Payload:
{
  "sub": "uuid-do-usuario",
  "email": "user@email.com",
  "role": "USER",
  "provider": "GOOGLE",
  "iat": timestamp,
  "exp": timestamp
}

Tempos de vida:
- accessToken: 1 hora
- refreshToken: 7 dias (renovável via POST /auth/refresh)

O gateway valida o JWT localmente usando a chave pública do JWKS endpoint — sem latência adicional por chamada ao assine-auth a cada requisição.

## Tabelas (assine-auth)

```sql
CREATE TABLE auth_users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    provider        VARCHAR(20) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at      TIMESTAMP NOT NULL,
    last_login      TIMESTAMP
);

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
```

## Consequências

### Positivas
- Sem gerenciamento de senhas: sem hash, sem reset, sem vazamento
- Account linking transparente: o usuário nunca precisa "vincular contas" manualmente — o sistema resolve pelo email
- JWT com RS256 e JWKS permite validação local no gateway sem dependência de disponibilidade do assine-auth por requisição
- Magic link e Google OAuth2 cobrem usuários com e sem conta Google

### Negativas
- Magic link depende de disponibilidade do provedor de email (SendGrid) — se o email não chegar, o usuário não consegue logar
- Account linking por email pressupõe que o email é verificado pelo provider — o Google garante isso; magic link por definição também (só quem tem acesso ao email pode validar o token)
- Refresh token exige armazenamento seguro no cliente

### Riscos e mitigações
- Risco: token de magic link interceptado
  Mitigação: expiração de 15 minutos e uso único minimizam a janela de exploração
- Risco: usuário tenta criar segunda conta com mesmo email
  Mitigação: UNIQUE CONSTRAINT em auth_users.email — a constraint é a última linha de defesa, o account linking resolve antes
- Risco: Google retorna email não verificado
  Mitigação: verificar o campo email_verified no payload do Google UserInfo antes de prosseguir com o account linking

## Alternativas rejeitadas

### Usuário e senha
Adiciona superfície de ataque (hash, reset, vazamento) sem benefício para o perfil simples do usuário do Assine.

### Magic link apenas
Depende exclusivamente de disponibilidade do email. Google OAuth2 como alternativa aumenta resiliência e reduz fricção para usuários com conta Google.

### Provedor externo (Auth0, Keycloak)
Adequado para sistemas maiores. Para o Assine, adiciona dependência externa e custo sem ganho proporcional. Implementar auth interno demonstra mais domínio técnico para o objetivo de portfolio.
