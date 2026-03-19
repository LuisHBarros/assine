# Infrastructure

## The short version

This project runs on Docker Compose.

Not Kubernetes.

That is intentional.

Today the repository has a few real runnable services and several planned services. Compose gives us a clean local platform now, without inventing cluster complexity before the application is ready for it.

## What actually runs today

Application containers:

- `assine-auth`
- `assine-billing`
- `assine-subscriptions`
- `assine-access`
- `assine-notifications`
- `assine-fiscal`

Support containers:

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

Optional:

- `frontend`

The frontend is optional because it still expects a gateway and some API routes that do not exist yet.

## What does not run yet

These folders are still architecture or partial implementation, not real deployable services:

- `assine-gateway`

## Why Compose is the right choice here

Because it matches the current stage of the project.

What we need right now is:

- one command to start the platform
- stable container names for service-to-service calls
- isolated local databases
- a real message broker
- observability tools beside the apps
- low operational overhead

Compose gives all of that.

Kubernetes would add a lot of work that does not help us yet:

- manifests
- ingress rules
- secrets/config indirection
- health and rollout tuning
- cluster setup and maintenance
- more debugging layers

That tradeoff only starts to pay back when we truly need replicas, autoscaling, rolling deploys, or multi-node operations.

## The current mental model

Think about the infrastructure like this:

- Docker Compose is the local platform
- each implemented backend service gets its own container
- each implemented stateful service gets its own Postgres database
- RabbitMQ is the internal event backbone
- Prometheus, Grafana, and Zipkin are always available for visibility
- unfinished services stay out of the runtime until they are real

This keeps the environment honest. If a service is not implemented, Compose should not pretend otherwise.

## How to start it

1. Copy `.env.example` to `.env`
2. Fill in the values you actually need
3. Start the stack

Commands:

```bash
docker compose up -d
docker compose ps
```

If you also want the frontend:

```bash
docker compose --profile frontend up -d
```

To stop everything:

```bash
docker compose down
```

## The important ports

- `8086` -> auth API
- `8082` -> billing API
- `8081` -> subscriptions API
- `8083` -> access API
- `8084` -> notifications API
- `8085` -> fiscal API
- `15672` -> RabbitMQ management UI
- `5672` -> RabbitMQ AMQP
- `5437` -> auth Postgres
- `5434` -> billing Postgres
- `5433` -> subscriptions Postgres
- `5435` -> access Postgres
- `5436` -> fiscal Postgres
- `9000` -> MinIO API
- `9001` -> MinIO console
- `9090` -> Prometheus
- `3001` -> Grafana
- `9411` -> Zipkin
- `3000` -> frontend, only when the frontend profile is enabled

## How the files are organized

- [docker-compose.yml](C:\Users\luish\Documents\assine\docker-compose.yml)
  The real stack definition
- [docker-compose.override.yml](C:\Users\luish\Documents\assine\docker-compose.override.yml)
  Local host port exposure for development
- [infra/prometheus.yml](C:\Users\luish\Documents\assine\infra\prometheus.yml)
  Prometheus scrape targets
- [assine-auth/Dockerfile](C:\Users\luish\Documents\assine\assine-auth\Dockerfile)
  Auth container build
- [assine-billing/Dockerfile](C:\Users\luish\Documents\assine\assine-billing\Dockerfile)
  Billing container build
- [assine-subscriptions/Dockerfile](C:\Users\luish\Documents\assine\assine-subscriptions\Dockerfile)
  Subscriptions container build
- [assine-access/Dockerfile](C:\Users\luish\Documents\assine\assine-access\Dockerfile)
  Access container build
- [assine-notifications/Dockerfile](C:\Users\luish\Documents\assine\assine-notifications\Dockerfile)
  Notifications container build
- [assine-fiscal/Dockerfile](C:\Users\luish\Documents\assine\assine-fiscal\Dockerfile)
  Fiscal container build
- [frontend/Dockerfile](C:\Users\luish\Documents\assine\frontend\Dockerfile)
  Optional frontend container build
- [.env.example](C:\Users\luish\Documents\assine\.env.example)
  Local environment contract

## A few deliberate choices

### Internal services are not all exposed

The apps can talk to each other through the Compose network by container name. We only publish host ports where that helps local development.

### Auth uses schema auto-update for now

`assine-auth` still does not have proper tracked migrations, so the Compose setup uses Hibernate schema update to keep local startup simple.

That is a temporary dev-time choice, not the final production posture.

### Billing still has one internal HTTP dependency

The architecture docs say internal communication should be event-only.

In the code, `assine-billing` still has a client for subscriptions.

So the repo is not fully aligned with the intended architecture yet. The infra docs should say that clearly.

### Frontend is opt-in

The frontend exists and can be containerized, but it is ahead of the backend contracts.

That is why it is behind a Compose profile instead of being started by default.

## What "done" means for the current infra

The infra is in a good state for this project if:

- `docker compose config` is valid
- the stack starts with one command
- auth, billing, subscriptions, access, and fiscal have stable databases
- RabbitMQ is available
- metrics and tracing tools are reachable
- the runtime description matches the real codebase

## When Kubernetes becomes worth it

We should revisit Kubernetes later, not now.

It becomes worth the cost when one or more of these become true:

- we need multiple replicas per service
- we need rolling deploys with minimal downtime
- we need autoscaling
- we need stronger production secrets/config handling
- we need cluster-level scheduling and resilience
- most of the planned services are actually implemented

Until then, Compose is the right level of infrastructure.
