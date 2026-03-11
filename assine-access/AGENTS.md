# Agent Guidelines for assine-access

## Service Overview
Manages user access permissions and subscription-based access control. Listens to subscription events and provides access decisions via REST API.

## Build & Test Commands
```bash
mvn clean install
mvn test
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName
```

## Service Structure
- **domain/**: Core access logic
  - model/: AccessPermission
  - port/: Repository interfaces
- **application/**: AccessService
- **adapter/**:
  - in/web/: AccessController (GET /access/{userId})
  - in/messaging/: SubscriptionEventConsumer
  - out/persistence/: JPA repositories

## Event Consumption
Consumes from RabbitMQ:
- `assine.subscription.activated`
- `assine.subscription.canceled`
- `assine.subscription.past_due`

## Configuration
- Database: `jdbc:postgresql://postgres-access:5432/access`
- Port: 8083
- RabbitMQ: listens to subscription events

## Key Constraints
- No cross-service HTTP calls - receive state via events only
- Domain layer zero framework dependencies
- Structured JSON logging with correlation ID
