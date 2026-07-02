# 0001. Use A Monorepo

Status: Accepted

Date: Needs verification

## Context

AidFlow is a portfolio-oriented microservices platform with multiple services, shared local infrastructure, and documentation that must evolve together. The current repository contains:

- Spring Boot Java service: `gateway-service`
- Spring Boot Kotlin service: `identity-service`
- Spring Boot Java service: `campaign-service`
- Python/FastAPI service: `notification-service`
- Planned service folders for future work
- Docker Compose infrastructure
- Observability configuration
- Architecture and event documentation

For local development, the platform is expected to start as one system through `infrastructure/docker-compose.yml` and helper scripts.

## Decision

Use a single monorepo for AidFlow application services, infrastructure configuration, scripts, and documentation.

## Consequences

Benefits:

- Easier local development and portfolio review.
- One Docker Compose file can orchestrate the full system.
- Architecture, events, and observability docs can be updated with code changes.
- Cross-service changes are easier to inspect in one pull request.
- Future Codex sessions can reason about the full platform from one workspace.

Tradeoffs:

- Repository size can grow as services mature.
- Service-specific CI may need careful path filtering later.
- Teams would need ownership boundaries if this became a production multi-team project.

## Current Conventions

- Each service lives under `services/<service-name>`.
- Shared local infrastructure lives under `infrastructure/`.
- Technical docs live under `docs/`.
- Helper scripts live under `scripts/`.
- Service names and ports should not change unless explicitly requested.
