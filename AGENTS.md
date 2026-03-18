# Agent Guidelines for Assine Project

## Project Overview
This is a microservices-based subscription platform using Hexagonal Architecture (DDD) with Spring Boot, PostgreSQL, RabbitMQ, and Docker. Services communicate exclusively via events.

## Build & Test Commands

### Running the entire stack
```bash
docker-compose up -d
docker-compose down
```

### Building individual services (when implementations exist)
```bash
# For each service: assine-gateway, assine-auth, assine-subscriptions, assine-billing, assine-access, assine-notifications, assine-fiscal
cd assine-[service-name]
mvn clean install
```

### Running tests
```bash
cd assine-[service-name]
mvn test
```

### Running a single test
```bash
cd assine-[service-name]
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName
```

### Database migrations (when Flyway/Liquibase is added)
```bash
mvn flyway:migrate
```

## Architecture Pattern: Hexagonal (Ports & Adapters)

Each service follows this structure:
- **domain/**: Core business logic, NO framework dependencies
  - model/: Entities, Value Objects, Enums
  - event/: Domain events
  - port/:
    - in/: Use case interfaces (public contracts)
    - out/: Repository/publisher interfaces (infrastructure contracts)
- **application/**: Orchestrates domain logic
  - usecase/: Service implementations that use domain ports
- **adapter/**: Infrastructure details
  - in/web/: REST controllers
  - in/messaging/: Event consumers
  - out/persistence/: JPA repositories and entities
  - out/messaging/: Event publishers (RabbitMQ)
  - out/gateway/: External service adapters (Stripe, SendGrid, etc.)

## Code Style Guidelines

### Package Structure
All packages follow: `br.com.assine.[service-name]`
Example: `br.com.assine.subscriptions.domain.model.Subscription`

### Naming Conventions
- **Classes**: PascalCase (e.g., `SubscriptionService`, `PaymentEventConsumer`)
- **Interfaces**: PascalCase with descriptive names (e.g., `SubscriptionRepository`, `DomainEventPublisher`)
- **Methods**: camelCase starting with verb (e.g., `createSubscription`, `activateSubscription`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULT_PLAN_ID`)
- **Value Objects**: End with domain concept (e.g., `SubscriptionId`, `UserId`, `PaymentId`)

### Imports
- Organize groups: java.*, jakarta.*, org.springframework.*, other external, internal
- Use static imports sparingly
- No wildcard imports (avoid `import java.util.*`)

### Type Guidelines
- Use `record` for immutable Value Objects and DTOs
- Entities use regular classes with JPA annotations
- Use `Optional<T>` for repository return types that may be absent
- Domain events are immutable records with timestamp

### Error Handling
- Create domain-specific exceptions in domain/exception package
- Use `@ControllerAdvice` for global exception handling in adapters
- Return proper HTTP status codes (400 for client errors, 500 for server errors)
- Log errors with correlation ID for tracing

### Logging
- Use structured JSON logging (logstash-logback-encoder)
- Include correlation ID in all logs via MDC
- Log levels: ERROR for failures, WARN for recoverable issues, INFO for key events, DEBUG for details
- Example: `log.info("Subscription activated for userId={}", userId);`

### Domain Events
- Events are immutable records with `LocalDateTime createdAt`
- Event classes in domain/event package
- Publisher interface in domain/port/out/
- Implementations in adapter/out/messaging/
- Events published to RabbitMQ topic exchange: `assine.events`

### Repository Pattern
- Interface in domain/port/out/ with domain types
- Implementation in adapter/out/persistence/ with JPA
- Map entities to domain models in persistence adapter
- One repository per aggregate root

### Event-Driven Communication
- Services communicate ONLY via RabbitMQ events (no HTTP between internal services)
- Use Outbox Pattern in assine-billing and assine-fiscal for atomicity
- Routing keys: `assine.[domain].[action]` (e.g., `assine.payment.confirmed`)
- Each service has its own database - no cross-service schema access

### Correlation ID
- Generate unique correlation ID in gateway filter
- Propagate via `X-Correlation-ID` header in all HTTP calls
- Include in all log entries via MDC
- Add as field in domain events when relevant

### Configuration
- Use `application.yml` for service configuration
- Environment variables for secrets (API keys, webhook secrets)
- Each service uses separate database
- Database URLs: `jdbc:postgresql://[postgres-service]:5432/[service-name]`
- MinIO for object storage (assine-fiscal, assine-notifications)
  - MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, MINIO_BUCKET

### Testing Guidelines
- Unit tests for domain logic (no infrastructure)
- Integration tests for adapters (with testcontainers when possible)
- Test use cases via their ports
- Mock infrastructure dependencies in application layer tests
- Use descriptive test names (e.g., `shouldActivateSubscriptionWhenPaymentConfirmed`)

### Docker & Services
- Each service has its own Dockerfile
- Services run on different ports: gateway(8080), auth(8086), subscriptions(8081), billing(8082), access(8083), notifications(8084), fiscal(8085)
- Postgres databases on ports: 5433, 5437, 5434, 5435, 5436
- RabbitMQ on ports 5672 (AMQP) and 15672 (Management UI)

## Key Constraints
1. Domain layer MUST have zero framework dependencies
2. Cross-service communication exclusively via RabbitMQ events
3. Each service owns its database schema
4. Use Outbox Pattern for event publishing when persistence is involved
5. All HTTP calls between services go through gateway
6. Correlation ID must be propagated throughout request chain
7. Logs must be structured JSON with correlation ID


## Constraint Rules
- Create multiple AGENTS.md, in each folder, explaining the ideia of his files.
