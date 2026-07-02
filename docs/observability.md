# Observability

AidFlow includes a local observability stack for container metrics, service metrics, and Docker container logs.

## Start The Platform

From the repository root:

```bash
docker compose -f infrastructure/docker-compose.yml up --build
```

## Local URLs

| Tool | URL |
| --- | --- |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| cAdvisor | http://localhost:8089 |
| Loki | http://localhost:3100 |

Grafana credentials:

```text
user: aidflow
password: aidflow
```

## Prometheus

Prometheus targets:

```text
http://localhost:9090/targets
```

Useful starter queries:

```promql
container_memory_usage_bytes
```

```promql
rate(container_cpu_usage_seconds_total[1m])
```

Prometheus scrapes cAdvisor and the service actuator metrics endpoints when available:

- `cadvisor:8080`
- `gateway-service:8080/actuator/prometheus`
- `identity-service:8081/actuator/prometheus`
- `campaign-service:8082/actuator/prometheus`
- `notification-service:8090/metrics`

If a service does not expose `/actuator/prometheus`, that target will appear down until metrics are enabled for that service.

## Kafka Event Metrics

Application services expose semantic Kafka event metrics in addition to Redpanda broker metrics.

Published events by type:

```promql
sum by (service, event_type, aggregate_type, topic) (
  rate(aidflow_kafka_events_published_total{status="success"}[5m])
)
```

Publication failures:

```promql
sum by (service, event_type, aggregate_type, topic) (
  increase(aidflow_kafka_events_publication_failed_total{status="failed"}[15m])
)
```

Consumed events by consumer group:

```promql
sum by (service, consumer_group, event_type, aggregate_type, topic) (
  rate(aidflow_kafka_events_consumed_total{status="success"}[5m])
)
```

Processed events:

```promql
sum by (service, consumer_group, event_type, aggregate_type, topic) (
  rate(aidflow_kafka_events_processed_total{status="success"}[5m])
)
```

Processing failures:

```promql
sum by (service, consumer_group, event_type, aggregate_type, topic) (
  increase(aidflow_kafka_events_processing_failed_total{status="failed"}[15m])
)
```

Notification simulation throughput:

```promql
sum by (service, event_type, aggregate_type, consumer_group) (
  rate(aidflow_notifications_sent_total{status="success"}[5m])
)
```

95th percentile Kafka event processing duration:

```promql
histogram_quantile(
  0.95,
  sum by (le, service, event_type, handler) (
    rate(aidflow_kafka_event_processing_duration_seconds_bucket[5m])
  )
)
```

## Logs With Loki

Promtail reads Docker JSON logs from:

```text
/var/lib/docker/containers/*/*.log
```

It sends logs to Loki at:

```text
http://loki:3100/loki/api/v1/push
```

Basic Loki query:

```logql
{job="docker"}
```

Filter by service label:

```logql
{job="docker", service="campaign-service"}
```

Other useful service labels include:

- `gateway-service`
- `identity-service`
- `campaign-service`
- `notification-service`
- `redpanda`
- `postgres`

Filter logs by a request trace id:

```logql
{job="docker", service="campaign-service"} | json | traceId="YOUR_TRACE_ID"
```

Filter by logger/class:

```logql
{job="docker", service="campaign-service", logger="com.aidflow.campaign.infrastructure.http.CampaignController"}
```

Filter by severity:

```logql
{job="docker", service="campaign-service", severity="ERROR"}
```

Filter by structured event fields:

```logql
{job="docker", service="campaign-service"} | json | eventType="CampaignCreated"
```

```logql
{job="docker", service="campaign-service"} | json | aggregateId="YOUR_AGGREGATE_ID"
```

Kafka event publications:

```logql
{job="docker", service="campaign-service"} | json | message="Kafka event published"
```

Kafka event publication failures:

```logql
{job="docker", service="campaign-service"} | json | message="Kafka event publication failed"
```

Kafka event consumption and processing:

```logql
{job="docker", service="notification-service"} | json | message="Kafka event received"
```

```logql
{job="docker", service="notification-service"} | json | message="Kafka event processed"
```

Notification simulation logs:

```logql
{job="docker", service="notification-service"} | json | message="Simulated notification sent"
```

Kafka event processing failures:

```logql
{job="docker", service="notification-service"} | json | message="Kafka event processing failed"
```

To inspect logs in Grafana:

1. Open http://localhost:3000.
2. Log in with `aidflow` / `aidflow`.
3. Open Explore.
4. Select the `Loki` datasource.
5. Run `{job="docker"}` or filter a specific service with `{job="docker", service="campaign-service"}`.

HTTP services include an `X-Trace-Id` response header. Send the same header on a request to force a known trace id:

```bash
curl http://localhost:8080/campaigns \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "X-Trace-Id: local-test-trace-1"
```

## Health Checks

```bash
curl http://localhost:9090/-/ready
curl http://localhost:3100/ready
curl http://localhost:8089
```
