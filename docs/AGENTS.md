# Agent Guidelines for docs

## Folder Overview
This folder contains architecture, API, observability, and decision records for the Assine project. Documentation should describe the current implementation truthfully and separate implemented behavior from planned architecture.

## Files
- `decisions/`: ADRs that explain accepted architectural decisions
- `API.md`: API contracts and integration notes
- `INFRA.md`: plain-language infrastructure overview for the current runtime
- `infra-compose.md`: current Docker Compose runtime and known gaps

## Constraints
- Prefer updating docs when runtime reality changes
- Call out planned services explicitly instead of implying they already run
- Keep infra docs consistent with the root `docker-compose.yml`
