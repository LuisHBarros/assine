# Agent Guidelines for assine-gateway

## Service Overview
Spring Cloud Gateway that routes requests to internal services. Generates and propagates correlation IDs across all requests.

## Build & Test Commands
```bash
mvn clean install
mvn test
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName
```

## Service Structure
- **config/**: RouteConfig, SecurityConfig (JWT validation)
- **filter/**: CorrelationIdFilter (generates/propagates X-Correlation-ID)

## Routes
- subscriptions → http://subscriptions:8081
- billing → http://billing:8082
- access → http://access:8083

## Configuration
- Port: 8080
- Service URLs via environment variables

## Key Constraints
- All HTTP between services MUST go through gateway
- Generate unique correlation ID if not present
- Always include `X-Correlation-ID` header
- Validate JWT tokens
- Structured JSON logging with correlation ID
