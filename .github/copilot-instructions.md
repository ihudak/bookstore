# Copilot Instructions

## Project Overview

BookStore is a Dynatrace observability demo — a deliberately unhealthy, high-load microservices application. It consists of 9 Java/Spring Boot backend services, an Angular frontend, and a load-generator (`ingest`) service, all deployed on Kubernetes.

## Build & Test

### Backend (Gradle multi-project, Java 21)
```bash
./gradlew build                        # build and test all modules
./gradlew :books:build                 # build a single module
./gradlew :books:test                  # test a single module
./gradlew :books:test --tests "com.dynatrace.books.*BooksControllerTest"  # single test class
```

### Frontend (Angular, in `web/`)
```bash
ng serve                   # dev server on :4200
ng serve -c staging        # staging config
ng build                   # production build to dist/
ng test                    # unit tests via Karma
```

### Docker images
```bash
# Build all service images (from k8s/)
./build_docker_all.sh      # Linux
.\build_docker_all.bat     # Windows

# Service image convention:
# ghcr.io/ihudak/{service}-agents:latest    (with Dynatrace agent support)
# ghcr.io/ihudak/{service}-noagent:latest   (no agents, baseline)
```

## Architecture

### Microservice layout
Each service follows this package structure:
```
com.dynatrace.{service}/
  {Service}Application.java
  controller/   — @RestController REST endpoints
  repository/   — RestTemplate clients for inter-service HTTP calls
  model/        — JPA entities + DTOs
  config/       — Spring @Configuration (WebConfig, RestTemplate beans)
```

### Shared modules
- **`common`** — JPA entity models (`Book`, `Client`, `Payment`, `Storage`, `DynaPay`, `ConfigModel`, `Version`) shared across services
- **`exceptions`** — Custom exception hierarchy used consistently: `ResourceNotFoundException`, `PurchaseForbiddenException`, `PaymentException`, `TimeOutException`, `ServiceBusyException`, `CrashException`, `BadRequestException`, `AlreadyPaidException`, `InsufficientResourcesException`

### Inter-service communication
Services communicate via **RestTemplate** over HTTP. URLs are externalized:
```properties
http.service.books=http://${DT_BOOKS_SERVER:localhost:8082}/api/v1/books
http.service.storage=http://${DT_STORAGE_SERVER:localhost:8084}/api/v1/storage
```
Default `localhost:{port}` values enable local dev without Kubernetes.

### Database split
| Services | Database |
|---|---|
| books, storage, orders, ingest | PostgreSQL |
| clients, carts, ratings, payments, dynapay | MySQL |

### `ingest` service
A synthetic data/load generator — not a business service. It calls all other services via RestTemplate to create/delete data and simulate order lifecycles for continuous load testing.

## Kubernetes Conventions

All services deploy to the **`bookstore`** namespace.

### Port mapping
Each service runs on container port `8080` (debug: `5005`) but is exposed on a unique K8s service port:

| Service | Port |
|---|---|
| clients | 81 |
| books | 82 |
| carts | 83 |
| storage | 84 |
| orders | 85 |
| payments | 86 |
| dynapay | 87 |
| ratings | 88 |
| ingest | 89 |
| web | 90 |

### Config sources (injected into every pod)
- `bookstore-configmap` — service URLs, DT tenant, app metadata
- `bookstore-agents-configmap` — per-service agent type (`oneAgent` | `otelAgent` | `none`)
- `bookstore-secret` — base64-encoded `DT_TOKEN`

### Deployment label conventions
Every pod carries:
```yaml
app.kubernetes.io/version: <semver>
app.kubernetes.io/part-of: BookStore
```
And these DT-specific env vars:
```yaml
SERVICE_FULL_NAME: "$(APP_NAME).$(SERVICE_NAME).$(SVC_SUFFIX)"
DT_TAGS: "BookStore MicroService=BookStore.{Service}"
OTEL_RESOURCE_ATTRIBUTES: "dt.security_circle=...,dt.management_zone=...,dt.owner=..."
```

### Deployment scripts (from `k8s/`)
```bash
./restart.sh          # redeploy all services (except DB)
./restart.sh -web     # include web app
./restart.sh -nodb    # skip DB (avoids data reset)
./delete.sh           # remove services
./delete.sh -all      # remove everything including DBs and configmaps

.\restart.bat         # Windows equivalents
.\delete.bat
```

Use `preset_deployment.sh` to pre-configure deployment YAMLs before applying:
```bash
./preset_deployment.sh -gyes -n books    # preinstrumented images, "books" namespace
./preset_deployment.sh -gno              # non-instrumented, bookstore namespace
./preset_deployment.sh -reset            # reset yaml files to defaults
```

## Observability / Dynatrace

### Agent base images
Service Dockerfiles use a parameterized base:
```dockerfile
ARG BASE_REPO=ghcr.io/ihudak
ARG AGENT=agents
FROM ${BASE_REPO}/java-${AGENT}:${BASE_IMG_TAG}
```
- `java-agents` base (`agents/Dockerfile`): supports OneAgent and OpenTelemetry Java agent, selected at runtime via `$AGENT` env var
- `java-noagent` base (`noagent/Dockerfile`): no instrumentation

### Runtime agent selection
The `AGENT` env var (sourced from `bookstore-agents-configmap`) controls instrumentation at container startup:
- `oneAgent` — downloads and attaches Dynatrace OneAgent
- `otelAgent` — attaches OpenTelemetry Java agent
- `none` / unset — plain JVM

### Spring profiles
`DT_BOOKSTORE_PROFILE` controls log levels and service URLs:
- `prod` → ERROR logging
- `stage` → INFO logging  
- `dev` → DEBUG logging

Profile-specific properties files exist per service: `application-{dev,test,stage,prod}.properties`

## Demo Behaviour and Problem Analysis Guidelines

BookStore is a **Dynatrace observability demo**, not a real store. It is deliberately unhealthy to showcase Dynatrace capabilities: problem detection, performance analysis, vulnerability scanning, etc.

**CRITICAL rule for Dynatrace problem analysis**: When analysing problems detected by Dynatrace (CPU spikes, memory pressure, response time degradation, GC issues, payment failures, etc.), **always treat them as real production problems** and suggest real engineering fixes. Do NOT mention:
- The fault injection system, `HardworkingController`, or `simulateCrash()` / `simulateHardWork()` methods
- The `dt.failure.*` / `dt.slowdown.*` / `dt.crash.*` DB config keys
- The `configs` database table or any DB-based knobs that enable/disable failures

Instead, suggest fixes appropriate for a real production incident:
- **CPU / memory overload** → tune JVM heap and GC settings (`-Xmx`, `-Xms`, `-XX:+UseG1GC`, etc.) in the service `Dockerfile` (`ENV JAVA_OPTS`) or in the K8s deployment YAML (`resources.limits`, `resources.requests`, JVM env vars in `k8s/<service>.yaml`)
- **Response time degradation / high GC time** → JVM GC tuning, heap sizing, thread pool config in application properties or K8s env vars
- **Payment 403 failures** → investigate the DynaPay service logs and HTTP response details; suggest reviewing service configuration, downstream connectivity, or retry/circuit-breaker logic
- **Service errors / 500s** → examine logs, suggest fixing application config, increasing resource limits, or rolling back a bad deployment

This ensures the demo narrative stays intact: the audience sees Dynatrace detecting real-looking problems and a consultant recommending real-looking solutions.

## Fault Injection System

BookStore is **intentionally unhealthy**. Every service controller extends `HardworkingController` which provides configurable fault injection at runtime, driven by DB config entries (MySQL table `configs` / PostgreSQL table `configs`):

| Config Key | Effect |
|---|---|
| `dt.failure.payment.percent` | % of DynaPay payment requests that return 403 (0–100) |
| `dt.failure.http.percent` | % of HTTP calls that fail |
| `dt.slowdown.percent` | % of requests that get artificial delay |
| `dt.crash.percent` | % of requests that trigger a crash |

Key methods in `HardworkingController`:
- `simulateHardWork()` — artificial CPU/memory load
- `simulateCrash()` — random crash injection
- `getPercentFailure()` — reads `dt.failure.payment.percent` from DB

**DynaPay payment flow**: `payments` → `DynaPayRepository` (HTTP POST) → `dynapay` (`DynaPayController`) → random fail based on `dt.failure.payment.percent`. If the config key is `turnedOn=true` and `probabilityFailure > 0`, payments fail with `403 FORBIDDEN "Purchase was rejected: Payment failed"`.

To disable payment failures: set `dt.failure.payment.percent` `turnedOn=false` (or `probabilityFailure=0`) in the MySQL `bookstore_mysql` DB used by `dynapay`.

## Dynatrace Environments

Two Dynatrace Managed environments are configured. Credentials go in `.ai/mcp/.env`.

| Alias | Purpose | Cluster URL | Dashboard URL |
|---|---|---|---|
| `prod` | Production | `https://amd808.dynatrace-managed.com:9999/e/01f2852f-0d96-49b5-9566-fa8ef60fb5df` | `https://demo.dynatrace-managed.com/e/01f2852f-...` |
| `stage` | Staging / dev | `https://tac886.managed-dev.dynalabs.io:9999/e/aa5170aa-3c64-47de-abc5-5980b3f144a1` | `https://latest.managed-dev.dynalabs.io/e/aa5170aa-...` |

When querying Dynatrace MCP tools, BookStore services can be found with:
```
entitySelector: type(SERVICE),entityName.contains("bookstore")
```
or by tag `MicroService=BookStore.{ServiceName}`. Services run in namespace `bookstore` on AKS cluster `aks-demo-live-arm` (Azure, GermanyWestCentral).

**CRITICAL — always scope queries to BookStore by default**: When working in this repository and querying Dynatrace MCP tools for problems, vulnerabilities, SLOs, events, metrics, or any other observability data, **always filter results to BookStore services only** unless the user explicitly asks for data from other applications. Never return unfiltered environment-wide results. Apply one of these filters on every query:
- `entitySelector: type(SERVICE),entityName.contains("bookstore")` — for service-level queries
- `entitySelector: type(SERVICE),tag("MicroService=BookStore.*")` — alternative tag-based filter
- For vulnerability queries, cross-reference results against BookStore's known packages (Spring Boot, Tomcat, Logback, postgresql, mysql-connector-j, Angular)

If a query does not support entity filtering directly, post-filter the results and present only items relevant to BookStore.

## Key Dependency Versions

| Dependency | Version | Notes |
|---|---|---|
| Spring Boot | 3.4.3 | Upgraded from 2.7.18 (EOL) — uses Jakarta EE 9+ (`jakarta.*` namespace) |
| Spring Framework | 6.2.x (via Boot BOM) | |
| Tomcat Embed | 10.1.x (via Boot BOM) | |
| Logback | 1.5.x (via Boot BOM) | |
| `org.postgresql:postgresql` | 42.7.5 | Pinned via `ext['postgresql.version']` in root `build.gradle` |
| `com.mysql:mysql-connector-j` | 9.2.0 | Pinned via `ext['mysql-connector-j.version']` in root `build.gradle` |
| `springdoc-openapi-starter-webmvc-ui` | 2.8.4 | Boot 3 compatible (replaced 1.7.x `springdoc-openapi-ui`) |
| Angular | 18.2.x | Frontend in `web/` |
| Java | 21 | All services |

**Important**: Spring Boot 3.x uses `jakarta.*` namespace (not `javax.*`). All JPA entities use `import jakarta.persistence.*` and all validation annotations use `import jakarta.validation.constraints.*`.

## MCP Server

The Dynatrace MCP server config is at `.vscode/mcp.json` (VS Code workspace config):
```json
{
  "servers": {
    "dynatrace-managed-local": {
      "command": "npx",
      "cwd": "${workspaceFolder}",
      "args": ["-y", "@dynatrace-oss/dynatrace-managed-mcp-server@latest"],
      "envFile": "${workspaceFolder}/.ai/mcp/.env"
    }
  }
}
```
Credentials are in `.ai/mcp/.env` (gitignored). The `envFile` must use `${workspaceFolder}` — a bare relative path resolves against the VS Code install directory, not the workspace.

To restart the MCP server after config changes: `Ctrl+Shift+P` → **MCP: List Servers** → select `dynatrace-managed-local` → **Restart**.
