# Notification Service

Python/FastAPI service that consumes AidFlow domain events and logs simulated notifications.

## Local Development

Install dependencies:

```bash
pip install -e ".[test]"
```

Run the service:

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8090
```

Health check:

```bash
curl http://localhost:8090/health
```

Expected response:

```json
{
  "status": "UP",
  "service": "notification-service"
}
```

## Configuration

Environment variables:

| Name | Default |
| --- | --- |
| `SERVICE_NAME` | `notification-service` |
| `SERVICE_PORT` | `8090` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `KAFKA_TOPIC` | `aidflow.events` |
| `KAFKA_CONSUMER_GROUP` | `aidflow-notifications` |
| `KAFKA_DLQ_TOPIC` | `aidflow.events.dlq` |

## Event Handling

The service consumes events from `aidflow.events`.

Supported events:

- `CampaignCreated`: logs a simulated notification message.

Unknown event types are logged and ignored.

Invalid messages or handler failures are published to the DLQ topic `aidflow.events.dlq`.

## Tests

```bash
pytest
```
