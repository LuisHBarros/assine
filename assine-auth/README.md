# assine-auth

Serviço de autenticação com magic link e Google OAuth2, com account linking baseado em email.

## Responsabilidade

- Solicitar e validar magic links
- Autenticar via Google OAuth2
- Emitir tokens JWT (RS256)
- Expor JWKS para validação no gateway
- Account linking baseado em email (identidade canônica)

## Arquitetura

Segue o padrão hexagonal (Ports & Adapters):

```
domain/
├── model/          # AuthUser, AuthUserId, AuthProvider, UserRole, MagicToken
├── event/          # MagicLinkRequested
└── port/           # Use cases e repositórios

application/
└── usecase/        # RequestMagicLinkService, ValidateMagicLinkService, OAuthCallbackService

adapter/
├── in/web/         # AuthController
├── out/persistence/# JPA repositories
├── out/oauth/      # GoogleOAuthAdapter
└── out/messaging/  # MagicLinkEmailPublisher
```

## Configuração

Variáveis de ambiente:
- `SPRING_DATASOURCE_URL`: jdbc:postgresql://postgres-auth:5432/auth
- `SPRING_RABBITMQ_HOST`: rabbitmq
- `GOOGLE_CLIENT_ID`: cliente da API Google OAuth2
- `GOOGLE_CLIENT_SECRET`: secret da API Google OAuth2
- `JWT_PRIVATE_KEY`: chave privada RS256 (assinatura)
- `JWT_PUBLIC_KEY`: chave pública RS256 (JWKS)

## Account Linking

O email é a identidade canônica do usuário. Um usuário cadastrado via magic link pode autenticar via Google com o mesmo email e será reconhecido como a mesma conta.

Fluxo:
1. Extrair email do provider (magic link ou Google)
2. Buscar AuthUser pelo email na tabela auth_users
3. Se não existe: criar novo AuthUser com provider atual
4. Se existe: autenticar usuário existente (ignora provider original)

## Tabelas

- `auth_users`: usuários autenticados com account linking por email
- `magic_tokens`: tokens de magic link (uso único, expiração 15 min)

Consulte `tables.md` para definições SQL completas.
