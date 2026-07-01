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

If a service does not expose `/actuator/prometheus`, that target will appear down until metrics are enabled for that service.

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
{job="docker"} |= "traceId=YOUR_TRACE_ID"
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
