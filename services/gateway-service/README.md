# Gateway Service

Spring Cloud Gateway entry point for AidFlow.

- Runs on port `8080`.
- Routes `/auth/**` to `http://localhost:8081`.
- Authenticates protected routes through identity-service `GET /internal/auth/me`.
