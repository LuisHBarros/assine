# Agent Guidelines for assine-billing

## Service Overview
Processes payments via Stripe/Pagar.me webhooks. Uses Outbox Pattern for atomic event publishing after payment processing.

## Build & Test Commands
```bash
mvn clean install
mvn test
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName
mvn flyway:migrate
```

## Service Structure
- **domain/**: Payment logic
  - model/: Payment, PaymentId, IdempotencyKey, PaymentStatus
  - event/: PaymentConfirmed, PaymentFailed
  - port/: ProcessWebhookUseCase, PaymentRepository, OutboxRepository, PaymentGateway
- **application/**: ProcessWebhookService (validates HMAC + Outbox Pattern)
- **adapter/**:
  - in/web/: WebhookController
  - out/persistence/: PaymentJpaRepository, OutboxJpaRepository
  - out/messaging/: OutboxPublisher
  - out/gateway/: StripeGatewayAdapter

## Event Publishing
Publishes to RabbitMQ (via Outbox Pattern):
- `assine.payment.confirmed`
- `assine.payment.failed`

## Configuration
- Database: `jdbc:postgresql://postgres-billing:5432/billing`
- Port: 8082
- Webhook secret via env: `STRIPE_WEBHOOK_SECRET`

## Key Constraints
- Outbox Pattern required for event publishing atomicity
- Validate webhook HMAC signatures
- Idempotency keys for duplicate webhook handling
- Structured JSON logging with correlation ID
