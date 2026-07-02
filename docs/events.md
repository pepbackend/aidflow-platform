# Events

AidFlow uses Kafka-compatible domain events through Redpanda. This document is the event catalog and event-contract reference. For broader architecture context, see [PROJECT_SPEC.md](PROJECT_SPEC.md).

## Broker

Local broker:

- Redpanda
- Kafka bootstrap address in Docker Compose: `redpanda:9092`
- Host port: `localhost:9092`
- Redpanda metrics: `localhost:9644`

Current event topic:

- `aidflow.events`

Current dead-letter topic:

- `aidflow.events.dlq`

Current consumer group:

- `aidflow-notifications`

## Event Envelope

All domain events should use this envelope shape:

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

Fields:

- `eventId`: unique event identifier.
- `eventType`: event name, such as `CampaignCreated`.
- `aggregateType`: owning aggregate type, such as `Campaign`.
- `aggregateId`: owning aggregate id.
- `occurredAt`: timestamp when the event occurred.
- `version`: event schema version.
- `payload`: event-specific body.

## Kafka Key

Use the aggregate id as the Kafka key when possible.

Current implementation:

- campaign-service publishes `CampaignCreated` with campaign id as the key.

This keeps events for the same aggregate ordered within a partition.

## Implemented Events

### CampaignCreated

Status: Implemented

Producer:

- `campaign-service`

Consumer:

- `notification-service`

Topic:

- `aidflow.events`

Aggregate:

- `aggregateType`: `Campaign`
- `aggregateId`: campaign id

Payload:

```json
{
  "campaignId": "uuid",
  "name": "Flood response in Granollers",
  "description": "Volunteer coordination after heavy rain flooding",
  "location": "Granollers",
  "priority": "HIGH",
  "status": "ACTIVE",
  "createdBy": "user-id"
}
```

Current behavior:

1. campaign-service persists the campaign.
2. campaign-service records the event in its outbox table.
3. the scheduled outbox publisher sends the event to `aidflow.events`.
4. notification-service consumes the event.
5. notification-service logs `Kafka event received`.
6. notification-service simulates a notification and logs `Simulated notification sent`.
7. notification-service logs `Kafka event processed`.

## Planned Events

### TaskCreated

Status: Planned

Expected producer:

- `task-service`

Expected consumers:

- `notification-service`
- `matching-service`

Expected aggregate:

- `Task`

Expected purpose:

- Signal that a new volunteer task was created for a campaign.

### TaskAssigned

Status: Planned

Expected producer:

- `task-service` or `matching-service`, depending on final ownership decision

Expected consumers:

- `notification-service`
- `campaign-service` if campaign read models are introduced

Expected aggregate:

- `Task`

Expected purpose:

- Signal that a volunteer or team was assigned to a task.

### TaskCompleted

Status: Planned

Expected producer:

- `task-service`

Expected consumers:

- `notification-service`
- future reporting/read-model services if added

Expected aggregate:

- `Task`

Expected purpose:

- Signal that a task was completed.

### NotificationSent

Status: Planned

Expected producer:

- `notification-service`

Expected consumers:

- Needs verification

Expected aggregate:

- `Notification`

Expected purpose:

- Signal that a real notification was sent. This should only be added if notification sending becomes real domain behavior rather than the current simulation log and metric.

## Event Versioning Rules

- New event types should start at `version: 1`.
- Additive payload fields are preferred over breaking changes.
- Breaking schema changes should use a new version and keep compatibility during migration.
- Consumers should ignore unknown fields where practical.
- Event names should be past-tense facts, not commands.

## Logging Requirements

Publication logs:

- `Kafka event published`
- `Kafka event publication failed`

Consumption logs:

- `Kafka event received`
- `Kafka event processed`
- `Kafka event processing failed`

Useful event log fields:

- `eventId`
- `eventType`
- `aggregateType`
- `aggregateId`
- `topic`
- `partition`
- `offset`
- `consumerGroup`
- `handler`
- `processingResult`

## Metrics Requirements

Custom application metrics should use low-cardinality labels only.

Current Kafka event metrics:

- `aidflow_kafka_events_published_total`
- `aidflow_kafka_events_publication_failed_total`
- `aidflow_kafka_events_consumed_total`
- `aidflow_kafka_events_processed_total`
- `aidflow_kafka_events_processing_failed_total`
- `aidflow_kafka_event_processing_duration_seconds`
- `aidflow_notifications_sent_total`

Recommended labels:

- `service`
- `event_type`
- `aggregate_type`
- `topic`
- `consumer_group`
- `status`
- `handler`

Do not use these as Prometheus labels:

- `eventId`
- `aggregateId`
- `campaignId`
- `campaignName`
- `userId`
- `email`
