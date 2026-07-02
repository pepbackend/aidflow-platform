# AidFlow Platform

AidFlow is a polyglot, event-driven microservices platform designed to coordinate volunteers, campaigns, tasks and notifications during local emergency response initiatives.

The goal of this project is to demonstrate a realistic backend architecture using Java, Kotlin, Python, Spring Boot, FastAPI, PostgreSQL, Kafka, an API Gateway and AI-assisted workflows.

## Main goals

- Build a polyglot microservices architecture
- Use Java, Kotlin and Python in the same monorepo
- Implement centralized authentication and authorization through an API Gateway
- Use PostgreSQL for service-owned persistence
- Use Kafka for domain events
- Integrate an AI assistant service for emergency planning and task suggestions
- Provide a local development environment with Docker Compose

## Services

- `gateway-service`: API entry point, authentication and routing
- `identity-service`: users, roles and token validation
- `campaign-service`: emergency response campaigns
- `task-service`: volunteer tasks linked to campaigns
- `notification-service`: consumes events and sends simulated notifications
- `ai-assistant-service`: uses AI to generate emergency plans and task proposals

## Documentation

- [Project specification](docs/PROJECT_SPEC.md): current architecture, service conventions, observability, event model, and roadmap for future development work.
- [Architecture](docs/architecture.md): runtime topology, request flow, event flow, persistence, and service boundaries.
- [Events](docs/events.md): Kafka event envelope, implemented events, planned events, logging, and metrics rules.
- [Observability](docs/observability.md): Prometheus, Grafana, Loki, cAdvisor, Redpanda metrics, and useful queries.
- [Architecture decisions](docs/decisions): short ADRs explaining major technical choices.

## First MVP

The first functional flow will be:

1. Register and login as a coordinator
2. Create a campaign
3. Create a task inside that campaign
4. Publish a `TaskCreated` event
5. Consume the event from the notification service
