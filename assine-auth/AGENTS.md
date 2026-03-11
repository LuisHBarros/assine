# Agent Guidelines for assine-auth

## Service Overview
Authentication service handling magic link and Google OAuth2 with account linking based on email. Issues JWT tokens (RS256) with JWKS endpoint for gateway validation.

## Build & Test Commands
```bash
mvn clean install
mvn test
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName
mvn flyway:migrate
```

## Service Structure
- **domain/**: Auth logic
  - model/: AuthUser (aggregate root), AuthUserId, AuthProvider, UserRole, MagicToken
  - event/: MagicLinkRequested
  - port/in/: RequestMagicLinkUseCase, ValidateMagicLinkUseCase, OAuthCallbackUseCase
  - port/out/: AuthUserRepository, MagicTokenRepository, OAuthProvider
- **application/usecase/**: RequestMagicLinkService, ValidateMagicLinkService, OAuthCallbackService (includes account linking)
- **adapter/**:
  - in/web/: AuthController
  - out/persistence/: JPA repositories for auth_users and magic_tokens
  - out/oauth/: GoogleOAuthAdapter
  - out/messaging/: MagicLinkEmailPublisher

## Endpoints
- POST /auth/magic-link — request magic link via email
- GET /auth/magic-link/validate — validate token and issue JWT
- GET /auth/oauth2/google — redirect to Google OAuth2
- GET /auth/oauth2/google/callback — Google OAuth2 callback
- POST /auth/refresh — refresh access token
- GET /auth/.well-known/jwks.json — JWKS for gateway validation

## Event Publishing
Publishes to RabbitMQ:
- `assine.auth.magic-link-requested`

## Configuration
- Database: `jdbc:postgresql://postgres-auth:5432/auth`
- Port: 8086
- RabbitMQ: publishes magic link events
- Google OAuth2: client_id, client_secret
- JWT: RS256 with private key (auth) / public key (JWKS for gateway)

## Key Constraints
- Email is canonical identity — account linking based on email, not provider
- Magic tokens are single-use and expire in 15 minutes
- JWT issued with RS256 algorithm
- Account linking ensures same email never creates duplicate accounts
- Structured JSON logging with correlation ID
