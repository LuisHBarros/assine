# Agent Guidelines for assine-notifications

## Service Overview
Sends email notifications (via SendGrid) for subscription and payment events. No database - stateless service.

## Build & Test Commands
```bash
mvn clean install
mvn test
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName
```

## Service Structure
- **consumer/**: NotificationEventConsumer (all relevant events)
- **template/**: EmailTemplateResolver
- **adapter/**: SendGridAdapter

## Event Consumption
Consumes from RabbitMQ:
- `assine.payment.confirmed`
- `assine.subscription.activated`
- `assine.subscription.canceled`
- `assine.subscription.past_due`

## Configuration
- RabbitMQ: `SPRING_RABBITMQ_HOST`
- SendGrid API key via env: `SENDGRID_API_KEY`
- No database - stateless

## Key Constraints
- No database required
- Consume events only - no publishing
- Template-based email rendering
- SendGrid adapter for delivery
- Structured JSON logging with correlation ID from events
