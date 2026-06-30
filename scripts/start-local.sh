#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

"$ROOT_DIR/services/identity-service/gradlew" -p "$ROOT_DIR/services/identity-service" bootJar -x test
"$ROOT_DIR/services/campaign-service/gradlew" -p "$ROOT_DIR/services/campaign-service" bootJar -x test
"$ROOT_DIR/services/gateway-service/gradlew" -p "$ROOT_DIR/services/gateway-service" bootJar -x test

docker compose -f "$ROOT_DIR/infrastructure/docker-compose.yml" up -d --build
