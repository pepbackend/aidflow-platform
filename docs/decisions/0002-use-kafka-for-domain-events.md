# 0002. Use Kafka-Compatible Events For Domain Events

Status: Accepted

Date: Needs verification

## Context

AidFlow needs service boundaries while still allowing cross-service side effects. For example, campaign creation should be able to trigger notification behavior without campaign-service synchronously calling notification-service.

The local platform uses Redpanda, which provides Kafka-compatible APIs and metrics with a simpler local development footprint.

## Decision

Use Kafka-compatible domain events through Redpanda for asynchronous cross-service communication.

Current topic:

- `aidflow.events`

Current DLQ topic:

- `aidflow.events.dlq`

Current implemented event:

- `CampaignCreated`

## Consequences

Benefits:

- Services remain loosely coupled.
- Event consumers can be added without changing producers.
- Event activity is visible through both Redpanda broker metrics and application-level metrics.
- The platform demonstrates event-driven backend architecture.

Tradeoffs:

- Event schemas must be maintained.
- Consumers need idempotency and error handling as behavior grows.
- Local development requires Redpanda to be healthy.
- Observability must cover both broker-level and application-level event behavior.

## Implementation Notes

- campaign-service uses an outbox table and scheduled publisher for Kafka publication.
- notification-service consumes from `aidflow.events` with consumer group `aidflow-notifications`.
- notification-service publishes failed events to `aidflow.events.dlq`.
- Events should use the shared envelope documented in `docs/events.md`.
- Producers should use the aggregate id as the Kafka key when possible.

## Observability Rules

- Application services should emit structured logs for event publication, receipt, processing, and failure.
- Application services should expose semantic Prometheus metrics with `aidflow_` prefixes.
- Broker metrics from Redpanda are useful but not enough for business-level observability.
- Do not put `eventId`, `aggregateId`, user ids, email addresses, or campaign names in Prometheus labels.
