# Agent Guidelines for assine-subscriptions

## Service Overview
Core domain service for subscription lifecycle management. Aggregates payment events to manage subscription state.

## Build & Test Commands
```bash
mvn clean install
mvn test
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName
mvn flyway:migrate
```

## Service Structure
- **domain/**: Core subscription logic
  - model/: Subscription (aggregate root), SubscriptionId, SubscriptionStatus, Plan, UserId
  - event/: SubscriptionActivated, SubscriptionCanceled, SubscriptionPastDue
  - port/in/: ActivateSubscriptionUseCase, CancelSubscriptionUseCase, CreateSubscriptionUseCase
  - port/out/: SubscriptionRepository, DomainEventPublisher
- **application/usecase/**: ActivateSubscriptionService, CancelSubscriptionService, CreateSubscriptionService
- **adapter/**:
  - in/web/: SubscriptionController
  - in/messaging/: PaymentEventConsumer (listens to payment events)
  - out/persistence/: JPA repositories
  - out/messaging/: RabbitMQEventPublisher

## Event Consumption & Publishing
Consumes from RabbitMQ:
- `assine.payment.confirmed`
- `assine.payment.failed`

Publishes to RabbitMQ:
- `assine.subscription.activated`
- `assine.subscription.canceled`
- `assine.subscription.past_due`

## Configuration
- Database: `jdbc:postgresql://postgres-subscriptions:5432/subscriptions`
- Port: 8081

## Key Constraints
- Domain layer MUST have zero framework dependencies
- Repository pattern with domain types
- Aggregate root pattern for invariants
- State changes via domain events only
- Structured JSON logging with correlation ID
