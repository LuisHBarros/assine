# Agent Guidelines for infra

## Folder Overview
This folder contains local infrastructure assets shared by the repository root runtime. These files support Docker Compose orchestration and observability for the services that are currently implemented.

## Files
- `prometheus.yml`: scrape configuration for Spring Boot actuator metrics exposed by containerized services

## Constraints
- Keep configurations aligned with the real runnable services in the repository
- Prefer container DNS names from `docker-compose.yml` for service discovery
- Avoid adding configs for services that do not exist yet
