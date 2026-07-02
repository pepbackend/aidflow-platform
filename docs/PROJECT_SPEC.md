# AidFlow Platform Project Specification

This document is a technical reference for future AidFlow development work. It describes the current repository state, implemented architecture, conventions, and roadmap. It is intentionally factual and should not be treated as marketing documentation.

## 1. Project Purpose

AidFlow Platform is a local emergency coordination platform used to demonstrate backend engineering skills in a realistic microservices setting.

The project currently demonstrates:

- Polyglot microservices
- Event-driven architecture
- Kafka-compatible messaging through Redpanda
- Spring Boot services
- FastAPI/Python services
- PostgreSQL persistence
- Docker Compose local infrastructure
- Observability with Prometheus, Grafana, Loki, Promtail, cAdvisor, and Redpanda metrics
- Structured JSON logs
- Prometheus application metrics

The platform is also intended to grow toward an AI-assisted emergency coordination system with future matching and assistant services.

## 2. Current Services

### gateway-service

- Language/framework: Java 21, Spring Boot 3.5, Spring Cloud Gateway WebFlux
- Responsibility: External HTTP entry point, request routing, trace id propagation, JWT validation through identity-service, user header injection
- Container name: `aidflow-gateway-service`
- Exposed port: `8080`
- Health endpoint: `/actuator/health`
- Metrics endpoint: `/actuator/prometheus`
- Main dependencies: Spring Cloud Gateway, Spring Boot Actuator, Micrometer Prometheus registry, logstash-logback-encoder
- PostgreSQL: No
- Kafka: Does not publish or consume events
- Important routes:
  - `/auth/**` routes to identity-service and bypasses gateway auth
  - `/campaigns/**` routes to campaign-service and requires gateway auth
- Internal behavior:
  - Calls `identity-service` at `/internal/auth/me`
  - Injects `X-User-Id`, `X-User-Email`, and `X-User-Roles`
  - Propagates or creates `X-Trace-Id`

### identity-service

- Language/framework: Kotlin, Java 21 toolchain, Spring Boot 3.5
- Responsibility: User registration, login, roles, JWT creation and validation
- Container name: `aidflow-identity-service`
- Internal port: `8081`
- Health endpoint: `/health` and `/actuator/health`
- Metrics endpoint: `/actuator/prometheus`
- Main dependencies: Spring Web, Spring Data JPA, Spring Validation, Spring Security Crypto, Flyway, PostgreSQL driver, Actuator, Micrometer Prometheus registry, logstash-logback-encoder
- PostgreSQL: Yes, database `identity_service`
- Kafka: Does not publish or consume events
- Important endpoints:
  - `POST /auth/register`
  - `POST /auth/login`
  - `GET /internal/auth/me`

### campaign-service

- Language/framework: Java 21, Spring Boot 3.5
- Responsibility: Campaign creation and campaign reads; records domain events through an outbox and publishes them to Kafka/Redpanda
- Container name: `aidflow-campaign-service`
- Internal port: `8082`
- Health endpoint: `/health` and `/actuator/health`
- Metrics endpoint: `/actuator/prometheus`
- Main dependencies: Spring Web, Spring Data JPA, Spring Kafka, Spring Validation, Flyway, PostgreSQL driver, Actuator, Micrometer Prometheus registry, logstash-logback-encoder
- PostgreSQL: Yes, database `campaign_service`
- Kafka: Publishes `CampaignCreated` events to topic `aidflow.events`
- Important endpoints:
  - `POST /campaigns`
  - `GET /campaigns`
  - `GET /campaigns/{id}`
- Event publication:
  - Uses an outbox table for pending events
  - Scheduled publisher sends pending events to Kafka
  - Uses campaign id as Kafka key

### notification-service

- Language/framework: Python 3.12+, FastAPI
- Responsibility: Consumes domain events and simulates notification sending
- Container name: `aidflow-notification-service`
- Exposed port: `8090`
- Health endpoint: `/health`
- Metrics endpoint: `/metrics`
- Main dependencies: FastAPI, aiokafka, prometheus-client, prometheus-fastapi-instrumentator, Pydantic, pydantic-settings, Uvicorn
- PostgreSQL: No
- Kafka: Consumes `aidflow.events` as consumer group `aidflow-notifications`; publishes failed events to DLQ topic `aidflow.events.dlq`
- Important endpoints:
  - `GET /health`
  - `GET /metrics`
- Implemented handlers:
  - `CampaignCreated` -> logs and simulates notification sending

## 3. Planned Services

### task-service - Planned

- Proposed language/framework: Spring Boot, likely Java or Kotlin
- Responsibility: Manage volunteer tasks linked to campaigns
- Expected interactions:
  - Synchronous HTTP through gateway for user-facing task workflows
  - May read campaign identifiers supplied by users or future service calls
  - Publishes task domain events to Kafka
- Expected events:
  - `TaskCreated`
  - `TaskAssigned`
  - `TaskCompleted`

### matching-service - Planned

- Proposed language/framework: Needs verification; likely Python/FastAPI or Spring Boot depending on matching complexity
- Responsibility: Match volunteers or resources to tasks/campaign needs
- Expected interactions:
  - Consume task/campaign events
  - Potentially publish recommendations or assignment proposals
  - May expose internal APIs for synchronous matching requests
- Expected events:
  - Needs verification
  - Possible future events include assignment recommendation or match result events

### ai-assistant-service - Planned

- Proposed language/framework: Python/FastAPI
- Responsibility: AI-assisted emergency planning, campaign/task suggestions, and operator support
- Expected interactions:
  - HTTP APIs for assistant workflows through gateway or internal calls
  - May consume domain context from campaigns/tasks
  - May propose tasks that are later persisted by task-service
- Expected events:
  - Needs verification
  - Possible future events include assistant recommendation events

## 4. Architecture Decisions

- Gateway handles external HTTP traffic.
- Gateway validates JWTs by calling identity-service.
- Internal services trust headers injected by the gateway:
  - `X-User-Id`
  - `X-User-Email`
  - `X-User-Roles`
- `X-Trace-Id` is used for request trace correlation.
- Kafka/Redpanda is used for domain events.
- Current domain event topic: `aidflow.events`.
- Current DLQ topic for notification-service: `aidflow.events.dlq`.
- PostgreSQL is used for service-owned persistence where needed.
- Services should expose health endpoints.
- Services should expose Prometheus metrics endpoints when practical.
- Services should use structured JSON logs.
- Prometheus metrics should avoid high-cardinality labels.
- `eventId` and `aggregateId` are allowed in logs but not as Prometheus labels.
- User-facing synchronous flows should use HTTP through gateway.
- Cross-service side effects should prefer events.
- Docker Compose is the source of truth for local orchestration.

## 5. Kafka / Event Model

Current event envelope fields:

```json
{
  "eventId": "uuid-or-string",
  "eventType": "CampaignCreated",
  "aggregateType": "Campaign",
  "aggregateId": "uuid-or-string",
  "occurredAt": "2026-07-02T10:00:00Z",
  "version": 1,
  "payload": {}
}
```

Field meaning:

- `eventId`: Unique event identifier.
- `eventType`: Event name, such as `CampaignCreated`.
- `aggregateType`: Aggregate category, such as `Campaign`.
- `aggregateId`: Aggregate identifier.
- `occurredAt`: Event occurrence timestamp.
- `version`: Event schema version.
- `payload`: Event-specific payload object.

Implemented events:

- `CampaignCreated`
  - Published by campaign-service.
  - Consumed by notification-service.
  - Current payload includes campaign fields such as `campaignId`, `name`, `description`, `location`, `priority`, `status`, and `createdBy`.

Planned events:

- `TaskCreated` - Planned
- `TaskAssigned` - Planned
- `TaskCompleted` - Planned
- `NotificationSent` - Planned if notification sending becomes a real domain event rather than only a simulation log/metric

## 6. Observability

Local observability stack:

- Prometheus: Scrapes infrastructure and service metrics. Local URL: `http://localhost:9090`
- Grafana: Dashboards and Explore UI for Prometheus and Loki. Local URL: `http://localhost:3000`
- Loki: Log storage/query backend. Local URL: `http://localhost:3100`
- Promtail: Reads Docker container logs and ships them to Loki.
- cAdvisor: Container CPU, memory, and runtime metrics. Local URL: `http://localhost:8089`
- Redpanda metrics: Broker/topic/consumer group metrics exposed on `redpanda:9644`.
- Kafka UI: Local Kafka/Redpanda inspection. Local URL: `http://localhost:8085`

Grafana credentials:

```text
user: aidflow
password: aidflow
```

Prometheus scrape targets include:

- `prometheus:9090`
- `redpanda:9644`
- `cadvisor:8080`
- `gateway-service:8080/actuator/prometheus`
- `identity-service:8081/actuator/prometheus`
- `campaign-service:8082/actuator/prometheus`
- `notification-service:8090/metrics`

Useful PromQL examples:

```promql
sum by (service, event_type, aggregate_type, topic, status) (
  rate(aidflow_kafka_events_published_total[5m])
)
```

```promql
sum by (service, event_type, aggregate_type, topic, status) (
  rate(aidflow_kafka_events_publication_failed_total[5m])
)
```

```promql
sum by (service, event_type, aggregate_type, topic, consumer_group, status) (
  rate(aidflow_kafka_events_consumed_total[5m])
)
```

```promql
sum by (service, event_type, aggregate_type, topic, consumer_group, status) (
  rate(aidflow_kafka_events_processed_total[5m])
)
```

```promql
sum by (service, event_type, aggregate_type, topic, consumer_group, status) (
  rate(aidflow_kafka_events_processing_failed_total[5m])
)
```

```promql
histogram_quantile(
  0.95,
  sum by (le, service, event_type, aggregate_type, topic, consumer_group, handler) (
    rate(aidflow_kafka_event_processing_duration_seconds_bucket[5m])
  )
)
```

```promql
sum by (service, event_type, aggregate_type, topic, consumer_group, status) (
  rate(aidflow_notifications_sent_total[5m])
)
```

```promql
rate(container_cpu_usage_seconds_total[1m])
```

```promql
container_memory_usage_bytes
```

Useful Redpanda metrics:

- `vectorized_cluster_partition_records_produced`
- `vectorized_cluster_partition_records_fetched`
- `vectorized_cluster_partition_bytes_produced_total`
- `vectorized_cluster_partition_bytes_fetched_total`
- `vectorized_kafka_group_offset`
- `vectorized_cluster_partition_high_watermark`

Useful LogQL examples:

```logql
{job="docker"}
```

```logql
{job="docker", service="campaign-service"} | json | message="Kafka event published"
```

```logql
{job="docker", service="campaign-service"} | json | message="Kafka event publication failed"
```

```logql
{job="docker", service="notification-service"} | json | message="Kafka event received"
```

```logql
{job="docker", service="notification-service"} | json | message="Kafka event processed"
```

```logql
{job="docker", service="notification-service"} | json | message="Kafka event processing failed"
```

```logql
{job="docker", service="notification-service"} | json | message="Simulated notification sent"
```

Current custom AidFlow metrics:

- `aidflow_kafka_events_published_total`
- `aidflow_kafka_events_publication_failed_total`
- `aidflow_kafka_events_consumed_total`
- `aidflow_kafka_events_processed_total`
- `aidflow_kafka_events_processing_failed_total`
- `aidflow_kafka_event_processing_duration_seconds`
- `aidflow_notifications_sent_total`
- `aidflow_notification_dlq_events_published_total`

Legacy/custom notification metrics:

- `aidflow_notification_kafka_events_received_total` was used previously; current code uses `aidflow_kafka_events_consumed_total`.

## 7. Logging Conventions

Services should log JSON to stdout/stderr for collection by Docker, Promtail, and Loki.

Common structured fields:

- `timestamp`
- `severity`
- `service`
- `logger`
- `thread`
- `message`
- `traceId` where available

Expected structured fields for Kafka event publication:

- `timestamp`
- `severity`
- `service`
- `logger`
- `message`
- `eventId`
- `eventType`
- `aggregateType`
- `aggregateId`
- `topic`
- `partition`
- `offset`
- `key` if available
- `errorType` on failure
- `errorMessage` on failure

Expected structured log messages:

- `Kafka event published`
- `Kafka event publication failed`
- `Kafka event received`
- `Kafka event processed`
- `Kafka event processing failed`
- `Simulated notification sent`

Kafka consumption logs should include:

- `eventId`
- `eventType`
- `aggregateType`
- `aggregateId`
- `topic`
- `partition`
- `offset`
- `consumerGroup`
- `handler` and `processingResult` for processed events

## 8. Metrics Conventions

Prometheus metric conventions:

- Prefix custom application metrics with `aidflow_`.
- Use snake_case metric names.
- Use bounded labels only.
- Prefer counters for event totals.
- Prefer histograms for latency/duration measurements.
- Do not include unique identifiers or unbounded user input in labels.

Recommended labels:

- `service`
- `event_type`
- `aggregate_type`
- `topic`
- `consumer_group`
- `status`
- `handler`

Labels that must not be used because they are high cardinality:

- `eventId`
- `aggregateId`
- `campaignId`
- `campaignName`
- `userId`
- `email`

## 9. Local Development

Start the local platform:

```bash
./scripts/start-local.sh
```

Stop the local platform:

```bash
./scripts/stop-local.sh
```

Direct Docker Compose command:

```bash
docker compose -f infrastructure/docker-compose.yml up --build
```

Useful commands:

```bash
docker ps
```

```bash
docker compose -f infrastructure/docker-compose.yml logs
```

```bash
docker logs aidflow-campaign-service
```

```bash
docker logs aidflow-notification-service
```

Health checks:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/health
curl http://localhost:8082/health
curl http://localhost:8090/health
```

Metrics endpoints:

```bash
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8081/actuator/prometheus
curl http://localhost:8082/actuator/prometheus
curl http://localhost:8090/metrics
```

Observability URLs:

- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`
- Loki: `http://localhost:3100`
- cAdvisor: `http://localhost:8089`
- Kafka UI: `http://localhost:8085`

## 10. Current Implemented Flow

1. User registers or logs in through identity-service via gateway.
2. Gateway routes `/auth/**` requests to identity-service.
3. Authenticated user creates a campaign through gateway.
4. Gateway validates the JWT by calling identity-service `/internal/auth/me`.
5. Gateway injects `X-User-Id`, `X-User-Email`, and `X-User-Roles`.
6. campaign-service persists the campaign in PostgreSQL.
7. campaign-service records a `CampaignCreated` event in the outbox.
8. campaign-service publishes `CampaignCreated` to Kafka/Redpanda topic `aidflow.events`.
9. notification-service consumes `CampaignCreated` from topic `aidflow.events`.
10. notification-service logs receipt and processing of the event.
11. notification-service simulates notification sending with a structured log.
12. Prometheus collects infrastructure and application metrics.
13. Loki collects Docker logs through Promtail.
14. Grafana visualizes logs and metrics.

## 11. Development Guidelines For Future Codex Tasks

- Read this file before making architectural or implementation changes.
- Inspect existing code before adding new abstractions.
- Do not duplicate existing metrics or logs.
- Keep services small and focused.
- Prefer simple hexagonal architecture in domain services.
- Use events for cross-service side effects.
- Use HTTP only for synchronous user-facing flows.
- Keep Docker Compose working.
- Keep observability working after each change.
- Add or update docs when implementing new services or events.
- Add tests where reasonable.
- Do not introduce heavy dependencies without a clear reason.
- Do not change ports, topic names, or service names unless explicitly requested.
- Preserve structured JSON logging conventions.
- Avoid high-cardinality Prometheus labels.
- Mark planned or uncertain behavior as `Planned` or `Needs verification`.

## 12. Roadmap

Implemented:

- Gateway routing
- Gateway JWT validation through identity-service
- Identity registration/login
- Campaign creation
- Campaign read endpoints
- Campaign outbox persistence
- Kafka publishing for `CampaignCreated`
- Notification consumer for `CampaignCreated`
- Notification DLQ publishing for failed event handling
- Local Docker Compose platform
- Observability stack
- Structured JSON logs
- Custom Kafka event metrics

Next:

- task-service
- `TaskCreated` event
- notification-service consumes `TaskCreated`
- matching-service
- ai-assistant-service
- Kubernetes manifests
- Terraform infrastructure

## How To Use This File

Future Codex sessions should read this file before making architectural or implementation changes. Treat it as the current factual map of AidFlow: service names, ports, event topics, implemented features, conventions, and planned work. If implementation changes make this document inaccurate, update this file in the same task.
