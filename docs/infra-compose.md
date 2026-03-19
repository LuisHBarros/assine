# Current Docker Compose Runtime

For the simpler, human-first overview, read `INFRA.md` first.

## Implemented stack

The repository can currently start these application services with Docker Compose:

- `assine-auth`
- `assine-billing`
- `assine-subscriptions`
- `assine-access`
- `assine-notifications`
- `assine-fiscal`

Supporting infrastructure started by Compose:

- RabbitMQ
- PostgreSQL for auth
- PostgreSQL for billing
- PostgreSQL for subscriptions
- PostgreSQL for access
- PostgreSQL for fiscal
- MinIO
- Prometheus
- Grafana
- Zipkin

## Optional services

- `frontend` has a Dockerfile and an optional Compose profile: `docker compose --profile frontend up -d`

The frontend is not part of the default stack because it still assumes a gateway and API routes that are not fully implemented in this repository.

## Planned but not runnable yet

These folders do not currently contain runnable Spring Boot services:

- `assine-gateway`

## Commands

Start the current stack:

```bash
docker compose up -d
```

Start the stack with the optional frontend:

```bash
docker compose --profile frontend up -d
```

Stop everything:

```bash
docker compose down
```

## Ports

- `auth`: `8086`
- `billing`: `8082`
- `subscriptions`: `8081`
- `access`: `8083`
- `notifications`: `8084`
- `fiscal`: `8085`
- `rabbitmq`: `5672`
- `rabbitmq management`: `15672`
- `postgres-auth`: `5437`
- `postgres-billing`: `5434`
- `postgres-subscriptions`: `5433`
- `postgres-access`: `5435`
- `postgres-fiscal`: `5436`
- `minio api`: `9000`
- `minio console`: `9001`
- `prometheus`: `9090`
- `grafana`: `3001`
- `zipkin`: `9411`

## Known gaps

- The documented event-only architecture is not fully implemented yet. `assine-billing` still contains an internal HTTP client for subscriptions.
- The frontend expects gateway-style routes and some auth and billing endpoints that do not exist yet.
- `assine-auth` uses `hibernate.ddl-auto=update` in Compose because it does not yet have tracked database migrations.
