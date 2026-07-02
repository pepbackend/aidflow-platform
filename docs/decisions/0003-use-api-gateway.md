# 0003. Use An API Gateway

Status: Accepted

Date: Needs verification

## Context

AidFlow has multiple backend services but should expose a simple external HTTP entry point for clients and local testing. Authentication should be centralized so downstream services do not each implement the same JWT validation flow.

The current implementation uses `gateway-service` with Spring Cloud Gateway.

## Decision

Use `gateway-service` as the external HTTP entry point.

The gateway is responsible for:

- Routing external HTTP traffic.
- Allowing unauthenticated `/auth/**` requests to identity-service.
- Validating protected requests by calling identity-service.
- Injecting user identity headers for downstream services.
- Creating or propagating `X-Trace-Id`.

## Current Routes

- `/auth/**` -> identity-service
- `/campaigns/**` -> campaign-service

## Identity Headers

Downstream services trust these gateway-injected headers:

- `X-User-Id`
- `X-User-Email`
- `X-User-Roles`

The gateway validates the client JWT by calling:

- `GET /internal/auth/me` on identity-service

## Consequences

Benefits:

- One external HTTP entry point.
- Centralized authentication enforcement.
- Downstream services can focus on business logic.
- Request trace ids are consistently propagated.
- Future routes can be added without changing clients' service discovery model.

Tradeoffs:

- Gateway availability becomes important for user-facing APIs.
- Downstream services rely on the gateway trust boundary.
- Internal endpoints should not be exposed publicly without the gateway in front.

## Rules For Future Work

- Add new user-facing routes through gateway-service.
- Keep `/auth/**` as the authentication route prefix unless explicitly changed.
- Do not require downstream services to parse JWTs unless the architecture is intentionally changed.
- Preserve `X-Trace-Id` propagation.
- Keep injected identity headers stable unless every downstream consumer is updated.
