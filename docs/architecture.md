# Architecture

AidFlow is a local, event-driven microservices platform for emergency coordination workflows. This document gives a focused architecture view. For the full repository reference, see [PROJECT_SPEC.md](PROJECT_SPEC.md).

## Runtime Topology

Local development runs through Docker Compose in `infrastructure/docker-compose.yml`.

Core application services:

- `gateway-service`: external HTTP entry point on `localhost:8080`
- `identity-service`: authentication and JWT validation on internal port `8081`
- `campaign-service`: campaign persistence and event publishing on internal port `8082`
- `notification-service`: Kafka event consumer and simulated notification sender on `localhost:8090`

Infrastructure services:

- `postgres`: PostgreSQL 16 on `localhost:5432`
- `redpanda`: Kafka-compatible broker on `localhost:9092`, metrics on `localhost:9644`
- `kafka-ui`: Kafka UI on `localhost:8085`
- `prometheus`: metrics storage/query on `localhost:9090`
- `grafana`: dashboards and Explore on `localhost:3000`
- `loki`: log storage on `localhost:3100`
- `promtail`: Docker log collector
- `cadvisor`: container metrics on `localhost:8089`

## Request Flow

External HTTP traffic enters through `gateway-service`.

Implemented routes:

- `/auth/**` routes to `identity-service`
- `/campaigns/**` routes to `campaign-service`

Authentication flow:

1. Client calls `POST /auth/register` or `POST /auth/login` through the gateway.
2. identity-service returns a JWT.
3. For protected routes, gateway calls identity-service `GET /internal/auth/me`.
4. If the JWT is valid, gateway forwards the request with identity headers:
   - `X-User-Id`
   - `X-User-Email`
   - `X-User-Roles`
5. Downstream services trust those gateway-injected headers.

Trace correlation:

- gateway creates or propagates `X-Trace-Id`.
- Spring services place the trace id into JSON logs.
- notification-service uses `X-Trace-Id` for HTTP requests and structured logs where available.

## Event Flow

Kafka-compatible messaging is provided by Redpanda.

Current topic:

- `aidflow.events`

Current DLQ topic:

- `aidflow.events.dlq`

Implemented event flow:

1. campaign-service persists a campaign.
2. campaign-service records a `CampaignCreated` event in its outbox table.
3. the scheduled outbox publisher sends the event to `aidflow.events`.
4. notification-service consumes the event as consumer group `aidflow-notifications`.
5. notification-service logs receipt and processing, then simulates notification sending.
6. failed notification-service event handling is published to `aidflow.events.dlq`.

## Persistence

PostgreSQL is service-owned. Services should not directly share tables.

Current databases:

- `identity_service`: users, roles, credentials, auth data
- `campaign_service`: campaigns and outbox events

Current non-persistent services:

- `gateway-service`
- `notification-service`

## Service Boundaries

gateway-service:

- Owns external routing and authentication enforcement.
- Does not own business state.
- Does not publish or consume Kafka events.

identity-service:

- Owns users, roles, password hashing, JWT issuing, and token validation.
- Provides internal identity validation to the gateway.

campaign-service:

- Owns campaign data.
- Publishes campaign domain events.
- Uses an outbox table to decouple transaction persistence from Kafka publishing.

notification-service:

- Owns event consumption and notification simulation.
- Does not persist state currently.
- Exposes custom Prometheus business metrics.

## Cross-Service Rules

- Use HTTP through the gateway for synchronous user-facing workflows.
- Use Kafka events for cross-service side effects.
- Avoid direct database coupling between services.
- Keep event payloads explicit and versioned.
- Do not use high-cardinality values as metric labels.
- Keep JSON structured logs stable enough for Loki queries.

## Planned Architecture

Planned services:

- `task-service`: task management for campaigns.
- `matching-service`: volunteer/resource matching.
- `ai-assistant-service`: AI-assisted emergency planning and suggestions.

Planned platform work:

- Kubernetes manifests.
- Terraform infrastructure.
- More Grafana dashboards.
- Expanded event catalog for tasks, assignments, completion, and real notifications.
