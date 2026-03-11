# Assine — Estrutura do Repositório

```
assine/
│
├── docker-compose.yml                  # Orquestra tudo localmente
├── docker-compose.override.yml         # Overrides para dev (hot reload, etc.)
├── .env.example                        # Variáveis de ambiente documentadas
├── README.md
│
├── assine-gateway/                     # Spring Cloud Gateway
│   ├── src/main/java/br/com/assine/gateway/
│   │   ├── config/
│   │   │   ├── RouteConfig.java        # Definição das rotas
│   │   │   └── SecurityConfig.java     # Validação JWT
│   │   └── filter/
│   │       └── CorrelationIdFilter.java # Gera/propaga X-Correlation-ID
│   └── src/main/resources/
│       └── application.yml
│
├── assine-auth/                        # Autenticação e emissão de JWT
│   ├── src/main/java/br/com/assine/auth/
│   │   │
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── AuthUser.java           # Agregado raiz
│   │   │   │   ├── AuthUserId.java         # Value Object
│   │   │   │   ├── AuthProvider.java       # Enum: MAGIC_LINK | GOOGLE
│   │   │   │   ├── UserRole.java           # Enum: USER | ADMIN
│   │   │   │   └── MagicToken.java         # token, email, expiresAt, used
│   │   │   ├── event/
│   │   │   │   └── MagicLinkRequested.java
│   │   │   └── port/
│   │   │       ├── in/
│   │   │       │   ├── RequestMagicLinkUseCase.java
│   │   │       │   ├── ValidateMagicLinkUseCase.java
│   │   │       │   └── OAuthCallbackUseCase.java
│   │   │       └── out/
│   │   │           ├── AuthUserRepository.java
│   │   │           ├── MagicTokenRepository.java
│   │   │           └── OAuthProvider.java      # porta para Google OAuth2
│   │   │
│   │   ├── application/
│   │   │   └── usecase/
│   │   │       ├── RequestMagicLinkService.java
│   │   │       ├── ValidateMagicLinkService.java
│   │   │       └── OAuthCallbackService.java   # inclui account linking
│   │   │
│   │   └── adapter/
│   │       ├── in/
│   │       │   └── web/
│   │       │       └── AuthController.java
│   │       │           # POST /auth/magic-link
│   │       │           # GET  /auth/magic-link/validate
│   │       │           # GET  /auth/oauth2/google
│   │       │           # GET  /auth/oauth2/google/callback
│   │       │           # POST /auth/refresh
│   │       │           # GET  /auth/.well-known/jwks.json
│   │       └── out/
│   │           ├── persistence/
│   │           │   ├── AuthUserJpaRepository.java
│   │           │   ├── AuthUserEntity.java
│   │           │   ├── MagicTokenJpaRepository.java
│   │           │   └── MagicTokenEntity.java
│   │           ├── oauth/
│   │           │   └── GoogleOAuthAdapter.java
│   │           └── messaging/
│   │               └── MagicLinkEmailPublisher.java
│   │
│   └── src/main/resources/
│       └── application.yml
│
├── assine-subscriptions/               # Core do domínio
│   ├── src/main/java/br/com/assine/subscriptions/
│   │   │
│   │   ├── domain/                     # SEM dependências de framework
│   │   │   ├── model/
│   │   │   │   ├── Subscription.java       # Agregado raiz
│   │   │   │   ├── SubscriptionId.java     # Value Object
│   │   │   │   ├── SubscriptionStatus.java # Enum: PENDING|ACTIVE|PAST_DUE|CANCELED
│   │   │   │   ├── Plan.java               # Agregado
│   │   │   │   └── UserId.java             # Value Object
│   │   │   ├── event/
│   │   │   │   ├── SubscriptionActivated.java
│   │   │   │   ├── SubscriptionCanceled.java
│   │   │   │   └── SubscriptionPastDue.java
│   │   │   └── port/
│   │   │       ├── in/
│   │   │       │   ├── ActivateSubscriptionUseCase.java
│   │   │       │   ├── CancelSubscriptionUseCase.java
│   │   │       │   └── CreateSubscriptionUseCase.java
│   │   │       └── out/
│   │   │           ├── SubscriptionRepository.java
│   │   │           └── DomainEventPublisher.java
│   │   │
│   │   ├── application/                # Orquestra o domínio
│   │   │   └── usecase/
│   │   │       ├── ActivateSubscriptionService.java
│   │   │       ├── CancelSubscriptionService.java
│   │   │       └── CreateSubscriptionService.java
│   │   │
│   │   └── adapter/                    # Detalhes de infraestrutura
│   │       ├── in/
│   │       │   ├── web/
│   │       │   │   └── SubscriptionController.java
│   │       │   └── messaging/
│   │       │       └── PaymentEventConsumer.java  # Consome PaymentConfirmed
│   │       └── out/
│   │           ├── persistence/
│   │           │   ├── SubscriptionJpaRepository.java
│   │           │   ├── SubscriptionEntity.java
│   │           │   └── SubscriptionPersistenceAdapter.java
│   │           └── messaging/
│   │               └── RabbitMQEventPublisher.java
│   │
│   └── src/main/resources/
│       └── application.yml
│
├── assine-billing/                     # Integração com Stripe/Pagar.me
│   ├── src/main/java/br/com/assine/billing/
│   │   │
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Payment.java
│   │   │   │   ├── PaymentId.java
│   │   │   │   ├── IdempotencyKey.java     # Value Object
│   │   │   │   └── PaymentStatus.java
│   │   │   ├── event/
│   │   │   │   ├── PaymentConfirmed.java
│   │   │   │   └── PaymentFailed.java
│   │   │   └── port/
│   │   │       ├── in/
│   │   │       │   └── ProcessWebhookUseCase.java
│   │   │       └── out/
│   │   │           ├── PaymentRepository.java
│   │   │           ├── OutboxRepository.java
│   │   │           └── PaymentGateway.java     # Porta para Stripe/Pagar.me
│   │   │
│   │   ├── application/
│   │   │   └── usecase/
│   │   │       └── ProcessWebhookService.java  # Valida HMAC + Outbox Pattern
│   │   │
│   │   └── adapter/
│   │       ├── in/
│   │       │   └── web/
│   │       │       └── WebhookController.java
│   │       └── out/
│   │           ├── persistence/
│   │           │   ├── PaymentJpaRepository.java
│   │           │   ├── OutboxJpaRepository.java  # Outbox Pattern
│   │           │   └── OutboxEntity.java
│   │           ├── messaging/
│   │           │   └── OutboxPublisher.java      # Lê outbox e publica na fila
│   │           └── gateway/
│   │               └── StripeGatewayAdapter.java
│   │
│   └── src/main/resources/
│       └── application.yml
│
├── assine-access/                      # Gerencia permissões
│   ├── src/main/java/br/com/assine/access/
│   │   ├── domain/
│   │   │   └── model/
│   │   │       └── AccessPermission.java
│   │   ├── application/
│   │   │   └── AccessService.java
│   │   └── adapter/
│   │       ├── in/
│   │       │   ├── web/
│   │       │   │   └── AccessController.java    # GET /access/{userId}
│   │       │   └── messaging/
│   │       │       └── SubscriptionEventConsumer.java
│   │       └── out/
│   │           └── persistence/
│   │               └── AccessPermissionJpaRepository.java
│   └── src/main/resources/
│       └── application.yml
│
  └── assine-notifications/               # Executa comunicação
    ├── src/main/java/br/com/assine/notifications/
    │   ├── consumer/
    │   │   └── NotificationEventConsumer.java  # Ouve todos os eventos relevantes
    │   ├── template/
    │   │   └── EmailTemplateResolver.java
    │   └── adapter/
    │       └── SendGridAdapter.java
    └── src/main/resources/
        └── application.yml

├── assine-fiscal/                      # Emissão de NFS-e
│   ├── src/main/java/br/com/assine/fiscal/
│   │   │
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Invoice.java            # Agregado raiz
│   │   │   │   ├── InvoiceId.java          # Value Object
│   │   │   │   └── InvoiceStatus.java      # PENDING|ISSUED|FAILED|CANCELED
│   │   │   ├── event/
│   │   │   │   ├── InvoiceIssued.java
│   │   │   │   └── InvoiceFailed.java
│   │   │   └── port/
│   │   │       ├── in/
│   │   │       │   └── IssueInvoiceUseCase.java
│   │   │       └── out/
│   │   │           ├── InvoiceRepository.java
│   │   │           ├── InvoiceOutboxRepository.java
│   │   │           ├── FiscalGateway.java      # porta para emissor externo
│   │   │           ├── InvoiceStorageGateway.java # porta para object storage
│   │   │           └── DomainEventPublisher.java
│   │   │
│   │   ├── application/
│   │   │   └── usecase/
│   │   │       └── IssueInvoiceService.java
│   │   │
│   │   └── adapter/
│   │       ├── in/
│   │       │   └── messaging/
│   │       │       └── PaymentConfirmedConsumer.java  # consome PaymentConfirmed
│   │       └── out/
│   │           ├── persistence/
│   │           │   ├── InvoiceJpaRepository.java
│   │           │   ├── InvoiceEntity.java
│   │           │   ├── InvoiceOutboxJpaRepository.java
│   │           │   └── InvoiceOutboxEntity.java
│   │           ├── messaging/
│   │           │   └── InvoiceOutboxPublisher.java
│   │           ├── fiscal/
│   │           │   └── NuvemFiscalGatewayAdapter.java
│   │           └── storage/
│   │               └── MinIOStorageAdapter.java  # implementa InvoiceStorageGateway
│   │
│   └── src/main/resources/
│       └── application.yml
```

---

## docker-compose.yml (esqueleto)

```yaml
version: '3.9'

services:

  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"     # Management UI
    environment:
      RABBITMQ_DEFAULT_USER: assine
      RABBITMQ_DEFAULT_PASS: assine

  postgres-subscriptions:
    image: postgres:16
    environment:
      POSTGRES_DB: subscriptions
      POSTGRES_USER: assine
      POSTGRES_PASSWORD: assine
    ports:
      - "5433:5432"

  postgres-billing:
    image: postgres:16
    environment:
      POSTGRES_DB: billing
      POSTGRES_USER: assine
      POSTGRES_PASSWORD: assine
    ports:
      - "5434:5432"

  postgres-access:
    image: postgres:16
    environment:
      POSTGRES_DB: access
      POSTGRES_USER: assine
      POSTGRES_PASSWORD: assine
    ports:
      - "5435:5432"

  postgres-fiscal:
    image: postgres:16
    environment:
      POSTGRES_DB: fiscal
      POSTGRES_USER: assine
      POSTGRES_PASSWORD: assine
    ports:
      - "5436:5432"

  postgres-auth:
    image: postgres:16
    environment:
      POSTGRES_DB: auth
      POSTGRES_USER: assine
      POSTGRES_PASSWORD: assine
    ports:
      - "5437:5432"

  auth:
    build: ./assine-auth
    ports:
      - "8086:8086"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-auth:5432/auth
      SPRING_RABBITMQ_HOST: rabbitmq
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
      JWT_PRIVATE_KEY: ${JWT_PRIVATE_KEY}
      JWT_PUBLIC_KEY: ${JWT_PUBLIC_KEY}
    depends_on:
      - postgres-auth
      - rabbitmq

  gateway:
    build: ./assine-gateway
    ports:
      - "8080:8080"
    environment:
      SUBSCRIPTIONS_URL: http://subscriptions:8081
      BILLING_URL: http://billing:8082
      ACCESS_URL: http://access:8083
      NOTIFICATIONS_URL: http://notifications:8084
    depends_on:
      - subscriptions
      - billing
      - access

  subscriptions:
    build: ./assine-subscriptions
    ports:
      - "8081:8081"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-subscriptions:5432/subscriptions
      SPRING_RABBITMQ_HOST: rabbitmq
    depends_on:
      - postgres-subscriptions
      - rabbitmq

  billing:
    build: ./assine-billing
    ports:
      - "8082:8082"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-billing:5432/billing
      SPRING_RABBITMQ_HOST: rabbitmq
      STRIPE_WEBHOOK_SECRET: ${STRIPE_WEBHOOK_SECRET}
    depends_on:
      - postgres-billing
      - rabbitmq

  access:
    build: ./assine-access
    ports:
      - "8083:8083"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-access:5432/access
      SPRING_RABBITMQ_HOST: rabbitmq
    depends_on:
      - postgres-access
      - rabbitmq

  notifications:
    build: ./assine-notifications
    environment:
      SPRING_RABBITMQ_HOST: rabbitmq
      SENDGRID_API_KEY: ${SENDGRID_API_KEY}
    depends_on:
      - rabbitmq

  fiscal:
    build: ./assine-fiscal
    ports:
      - "8085:8085"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-fiscal:5432/fiscal
      SPRING_RABBITMQ_HOST: rabbitmq
      NUVEM_FISCAL_CLIENT_ID: ${NUVEM_FISCAL_CLIENT_ID}
      NUVEM_FISCAL_CLIENT_SECRET: ${NUVEM_FISCAL_CLIENT_SECRET}
      NUVEM_FISCAL_ENVIRONMENT: sandbox
      MINIO_ENDPOINT: http://minio:9000
      MINIO_ACCESS_KEY: assine
      MINIO_SECRET_KEY: assine123
      MINIO_BUCKET: invoices
    depends_on:
      - postgres-fiscal
      - rabbitmq
      - minio

  minio:
    image: minio/minio:latest
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: assine
      MINIO_ROOT_PASSWORD: assine123
    command: server /data --console-address ":9001"
    volumes:
      - minio_data:/data
  ```
  
volumes:
  minio_data:

---

## Filas RabbitMQ (exchanges e routing keys)

```
Exchange: assine.events  (type: topic)

Billing publica:
  assine.payment.confirmed
  assine.payment.failed

Subscriptions publica:
  assine.subscription.activated
  assine.subscription.canceled
  assine.subscription.past_due

Fiscal publica:
  assine.invoice.issued
  assine.invoice.failed

Auth publica:
  assine.auth.magic-link-requested

Quem consome o quê:
  subscriptions  →  assine.payment.*
  access         →  assine.subscription.*
  fiscal         →  assine.payment.confirmed
  notifications  →  assine.payment.confirmed
                     assine.subscription.*
                     assine.invoice.issued
                     assine.invoice.failed
                     assine.auth.magic-link-requested
```

---

## Convenções do projeto

- Correlation ID propagado via header `X-Correlation-ID` em todas as chamadas HTTP e como campo nos logs (MDC)
- Logs estruturados em JSON (Logback + logstash-logback-encoder)
- Cada serviço tem seu próprio banco — sem acesso cruzado a schemas alheios
- Comunicação entre BCs exclusivamente via eventos (nunca HTTP entre serviços internos)
- Outbox Pattern no `assine-billing` para garantir atomicidade entre persistir pagamento e publicar evento
