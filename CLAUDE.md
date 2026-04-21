# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BookStore is a **Dynatrace observability demo** — a deliberately unhealthy, high-load microservices application used to showcase Dynatrace capabilities (problem detection, vulnerability scanning, performance analysis). It is **not** a real store. 9 Java/Spring Boot backend services + Angular frontend + load-generator, deployed on Kubernetes.

## Build & Test

### Backend (Gradle multi-project, Java 21)
```bash
./gradlew build                        # build and test all modules
./gradlew :books:build                 # build a single module
./gradlew :books:test                  # test a single module
./gradlew :books:test --tests "com.dynatrace.books.*BooksControllerTest"
```

### Frontend (Angular, in `web/`)
```bash
ng serve                   # dev server on :4200
ng serve -c staging
ng build                   # production build to dist/
ng test                    # unit tests via Karma
```

### Docker images (from `k8s/`)
```bash
./build_docker_all.sh      # Linux
.\build_docker_all.bat     # Windows
```
Image naming: `ghcr.io/ihudak/{service}-agents:latest` (with agent support) or `ghcr.io/ihudak/{service}-noagent:latest`.

## Architecture

### Microservice layout
Each Java service follows this package structure:
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
- **`exceptions`** — Custom exception hierarchy: `ResourceNotFoundException`, `PurchaseForbiddenException`, `PaymentException`, `TimeOutException`, `ServiceBusyException`, `CrashException`, `BadRequestException`, `AlreadyPaidException`, `InsufficientResourcesException`

### Inter-service communication
HTTP via **RestTemplate**. URLs are externalized in `application.properties`:
```properties
http.service.books=http://${DT_BOOKS_SERVER:localhost:8082}/api/v1/books
```
The `localhost:{port}` defaults enable local dev without Kubernetes.

### Database split
| Services | Database |
|---|---|
| books, storage, orders, ingest | PostgreSQL |
| clients, carts, ratings, payments, dynapay | MySQL |

### `ingest` service
Synthetic load generator — not a business service. Calls all other services via RestTemplate to simulate continuous order lifecycles for load testing.

### Key dependency versions
| Dependency | Version |
|---|---|
| Spring Boot | 2.7.18 on this branch (`javax.*` namespace); master uses Spring Boot 3.x (`jakarta.*`) |
| Java | 21 |
| PostgreSQL driver | pinned via `ext['postgresql.version']` in root `build.gradle` |
| MySQL Connector/J | pinned via `ext['mysql-connector-j.version']` in root `build.gradle` |
| Angular | 18.2.x |

### Spring profiles
`DT_BOOKSTORE_PROFILE` controls log levels: `prod` → ERROR, `stage` → INFO, `dev` → DEBUG. Profile-specific property files exist per service: `application-{dev,test,stage,prod}.properties`.

## Fault Injection System

Every service controller extends `HardworkingController`, which provides configurable fault injection driven by DB config entries (`configs` table in MySQL/PostgreSQL):

| Config Key | Effect |
|---|---|
| `dt.failure.payment.percent` | % of DynaPay requests returning 403 (0–100) |
| `dt.failure.http.percent` | % of HTTP calls that fail |
| `dt.slowdown.percent` | % of requests that receive artificial delay |
| `dt.crash.percent` | % of requests that trigger a crash |

Key methods: `simulateHardWork()` — artificial CPU/memory load; `simulateCrash()` — random crash injection.

**DynaPay payment flow**: `payments` → `DynaPayRepository` (HTTP POST) → `dynapay` (`DynaPayController`) → random fail based on `dt.failure.payment.percent`.

## Dynatrace Problem Analysis Guidelines

**CRITICAL**: When analyzing Dynatrace-detected problems, **always treat them as real production problems** and suggest real engineering fixes. **Never mention** the fault injection system, `HardworkingController`, `simulateCrash()`/`simulateHardWork()`, the `dt.failure.*`/`dt.slowdown.*`/`dt.crash.*` config keys, or the `configs` DB table. The demo narrative requires the audience to see real-looking problems with real-looking solutions.

Suggest instead:
- **CPU/memory overload** → tune JVM heap/GC in `Dockerfile` (`ENV JAVA_OPTS`) or K8s deployment YAML (`resources.limits`, JVM env vars)
- **Response time degradation / high GC** → JVM GC tuning, heap sizing, thread pool config
- **Payment 403 failures** → investigate DynaPay logs; review service config, downstream connectivity, retry/circuit-breaker logic
- **Service errors / 500s** → examine logs, fix app config, increase resource limits, or roll back

## Kubernetes

All services deploy to the **`bookstore`** namespace (default; overridable via `preset_deployment.sh`).

### Port mapping
All containers run on port `8080` (debug: `5005`), exposed on unique K8s service ports: clients=81, books=82, carts=83, storage=84, orders=85, payments=86, dynapay=87, ratings=88, ingest=89, web=90.

### Config sources (injected into every pod)
- `bookstore-configmap` — service URLs, DT tenant, app metadata
- `bookstore-agents-configmap` — per-service agent type (`oneAgent` | `otelAgent` | `none`)
- `bookstore-secret` — base64-encoded `DT_TOKEN`

### Deployment scripts (from `k8s/`)
```bash
./restart.sh          # redeploy all services (except DB)
./restart.sh -web     # include web app
./restart.sh -nodb    # skip DB (avoids data reset)
./delete.sh           # remove services
./delete.sh -all      # remove everything including DBs and configmaps

# Pre-configure deployment YAMLs before applying:
./preset_deployment.sh -gyes -n books   # preinstrumented images, custom namespace
./preset_deployment.sh -gno             # non-instrumented, bookstore namespace
./preset_deployment.sh -reset           # reset yaml files to defaults
```

### Observability agent selection
`AGENT` env var (from `bookstore-agents-configmap`) at container startup: `oneAgent` → Dynatrace OneAgent, `otelAgent` → OpenTelemetry Java agent, `none` → plain JVM.

Service Dockerfiles use a parameterized base:
```dockerfile
ARG BASE_REPO=ghcr.io/ihudak
ARG AGENT=agents
FROM ${BASE_REPO}/java-${AGENT}:${BASE_IMG_TAG}
```

## Dynatrace Environments & MCP

Two Dynatrace Managed environments. Credentials in `.ai/mcp/.env` (gitignored).

| Alias | Purpose |
|---|---|
| `prod` | Production — AKS cluster `aks-demo-live-arm`, GermanyWestCentral |
| `stage` | Staging / dev |

**Always scope Dynatrace queries to BookStore by default.** Use one of:
- `entitySelector: type(SERVICE),entityName.contains("bookstore")`
- `entitySelector: type(SERVICE),tag("MicroService=BookStore.*")`

When filtering by service name, use only the core named services (exclude `apponly`, `SpringBoot ...`, and OTel variants):
- BookStore-Books, BookStore-Orders, BookStore-Carts, BookStore-Storage, BookStore-Ratings, BookStore-Payments, BookStore-Dynapay, BookStore-Ingest, BookStore-Clients

### MCP server (VS Code)
Config: `.vscode/mcp.json`. To restart after config changes: `Ctrl+Shift+P` → **MCP: List Servers** → `dynatrace-managed-local` → **Restart**. If `dynatrace_managed_query_metrics_data` appears missing, restart the MCP server (stale npx cache) — never fall back to direct terminal API calls.

### dtctl
`dtctl` is a kubectl-style CLI for Dynatrace. Key patterns:
```bash
dtctl config current-context          # show active context
dtctl auth whoami --plain             # show authenticated user
dtctl get workflows --mine -o json --plain
dtctl query "fetch logs | limit 10" -o json --plain
dtctl exec workflow <id>
dtctl apply -f dashboard.yaml --plain
```
Use `--plain` or `--agent` for AI/script consumption. Use IDs (not names) to avoid ambiguity. Check `dtctl auth can-i <verb> <resource>` before destructive ops.

### Token safety
When a direct API call is unavoidable, never inline tokens — read them into a variable first:
```powershell
$token = (Get-Content .ai/mcp/.env | Where-Object { $_ -match "^DT_PROD_TOKEN" }).Split("=",2)[1]
Invoke-RestMethod -Headers @{ Authorization = "Api-Token $token" } -Uri "..."
```
