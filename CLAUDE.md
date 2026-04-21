# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BookStore is a **Dynatrace observability demo** — a deliberately unhealthy, high-load microservices application. Its purpose is to showcase Dynatrace problem detection, performance analysis, and vulnerability scanning. It is not a real store.

## Build Commands

### Backend (Gradle multi-project, Java 21)
```bash
./gradlew build                        # build and test all modules
./gradlew :books:build                 # build a single module
./gradlew :books:test                  # test a single module
./gradlew :books:test --tests "com.dynatrace.books.*BooksControllerTest"  # single test class
```

### Frontend (Angular 18, in `web/`)
```bash
ng serve                   # dev server on :4200
ng serve -c staging        # staging config
ng build                   # production build to dist/
ng test                    # unit tests via Karma
```

### Docker images (from `k8s/`)
```bash
./build_docker_all.sh      # Linux — builds all service images
.\build_docker_all.bat     # Windows
```
Image naming: `ghcr.io/ihudak/{service}-agents:latest` (with DT agent) / `ghcr.io/ihudak/{service}-noagent:latest`

### Kubernetes deployment (from `k8s/`)
```bash
./restart.sh          # redeploy all services (except DB)
./restart.sh -web     # include web app
./restart.sh -nodb    # skip DB (avoids data reset)
./delete.sh           # remove services
./delete.sh -all      # remove everything including DBs and configmaps
./preset_deployment.sh -gyes -n books   # configure YAMLs for preinstrumented images
./preset_deployment.sh -gno             # non-instrumented, bookstore namespace
./preset_deployment.sh -reset           # reset YAML files to defaults
```

## Architecture

### Gradle modules
- **`common`** — JPA entity models (`Book`, `Client`, `Payment`, `Storage`, `DynaPay`, `ConfigModel`, `Version`) shared across services
- **`exceptions`** — Custom exception hierarchy: `ResourceNotFoundException`, `PurchaseForbiddenException`, `PaymentException`, `TimeOutException`, `ServiceBusyException`, `SecurityException`, `BadRequestException`, `AlreadyPaidException`, `InsufficientResourcesException`
- **9 Spring Boot services**: `books`, `clients`, `carts`, `storage`, `orders`, `ratings`, `payments`, `dynapay`, `ingest`

### Per-service package structure
```
com.dynatrace.{service}/
  {Service}Application.java
  controller/   — @RestController REST endpoints (extend SecurityController from common)
  repository/   — RestTemplate clients for inter-service HTTP calls + JPA repositories
  model/        — DTOs and service-local models
  config/       — Spring @Configuration (WebConfig, RestTemplate beans)
```

### Inter-service communication
Services call each other via **RestTemplate** over HTTP. URLs are externalized via env vars:
```properties
http.service.books=http://${DT_BOOKS_SERVER:localhost:8082}/api/v1/books
```
The `localhost:{port}` defaults enable local development without Kubernetes.

### Database split
| Services | Database |
|---|---|
| books, storage, orders, ingest | PostgreSQL |
| clients, carts, ratings, payments, dynapay | MySQL |

Tests use H2 (in-memory).

### `ingest` service
A synthetic load generator — not a business service. It continuously calls all other services via RestTemplate to simulate order lifecycles.

### `SecurityController` (base class in `common`)
All service controllers extend `SecurityController`. It implements DB-driven fault injection read from the `configs` table:
- `runThreatScan()` — simulates CPU/memory pressure based on `dt.work.hard` config
- `applySecurityPolicy()` — throws `SecurityException` based on `dt.simulate.crash` config
- `isInTimeWindow()` / `isWithinProbability()` — time-window and probabilistic activation

### Key dependency versions
| Dependency | Version |
|---|---|
| Spring Boot | 3.5.3 (Jakarta EE 9+, use `jakarta.*` not `javax.*`) |
| Java | 21 |
| PostgreSQL driver | pinned via `ext['postgresql.version']` in root `build.gradle` |
| MySQL driver | pinned via `ext['mysql-connector-j.version']` in root `build.gradle` |
| Angular | 18.2.x |

## Kubernetes Conventions

All services deploy to the **`bookstore`** namespace. Every service runs on container port `8080` (debug: `5005`) and is exposed on a unique K8s service port (clients=81, books=82, carts=83, storage=84, orders=85, payments=86, dynapay=87, ratings=88, ingest=89, web=90).

Config is injected via:
- `bookstore-configmap` — service URLs, DT tenant, app metadata
- `bookstore-agents-configmap` — per-service agent type (`oneAgent` | `otelAgent` | `none`)
- `bookstore-secret` — base64-encoded `DT_TOKEN`

### Spring profiles
`DT_BOOKSTORE_PROFILE` controls log level: `prod`=ERROR, `stage`=INFO, `dev`=DEBUG. Profile-specific files: `application-{dev,test,stage,prod}.properties`.

### Observability agent selection
The `AGENT` env var (from `bookstore-agents-configmap`) controls runtime instrumentation. Base images are in `agents/` (OneAgent + OTel support) and `noagent/`.

## Fault Injection System

BookStore is **intentionally unhealthy**. Fault injection is controlled by the `configs` DB table (`ConfigModel` entity) at runtime. Key config keys:

| Key | Effect |
|---|---|
| `dt.failure.payment.percent` | % of DynaPay payments that return 403 |
| `dt.failure.http.percent` | % of HTTP calls that fail |
| `dt.slowdown.percent` | % of requests that get artificial delay |
| `dt.crash.percent` | % of requests that trigger a crash |
| `dt.work.hard` | CPU/memory load simulation |
| `dt.simulate.crash` | Security exception injection |

**DynaPay payment flow**: `payments` → `DynaPayRepository` (HTTP POST) → `dynapay` (`DynaPayController`) → random fail if `dt.failure.payment.percent` `turnedOn=true` and `probabilityFailure > 0`.

## Dynatrace Problem Analysis — CRITICAL RULE

When analysing problems detected by Dynatrace (CPU spikes, memory pressure, response time degradation, payment failures, etc.), **always treat them as real production problems** and suggest real engineering fixes. **Never** mention the fault injection system, `SecurityController`, `simulateHardWork()`, `simulateCrash()`, the `configs` table, or any `dt.failure.*` / `dt.slowdown.*` / `dt.crash.*` DB config keys.

Suggest fixes appropriate for a real production incident:
- **CPU / memory overload** → tune JVM heap/GC in `Dockerfile` (`ENV JAVA_OPTS`) or K8s deployment YAML (`resources.limits`, `resources.requests`)
- **Response time degradation** → JVM GC tuning, heap sizing, thread pool config
- **Payment 403 failures** → investigate DynaPay service logs, review service config and downstream connectivity
- **Service errors / 500s** → examine logs, fix application config, increase resource limits, or roll back deployment

## Dynatrace MCP Server

Config: `.vscode/mcp.json`. Credentials: `.ai/mcp/.env` (gitignored).

When querying Dynatrace MCP tools, **always scope to BookStore** — never return unfiltered environment-wide results:
```
entitySelector: type(SERVICE),entityName.contains("bookstore")
entitySelector: type(SERVICE),tag("MicroService=BookStore.*")
```

Two environments:
- `prod`: `https://amd808.dynatrace-managed.com:9999/e/01f2852f-...`
- `stage`: `https://tac886.managed-dev.dynalabs.io:9999/e/aa5170aa-...`

If MCP tools appear missing, restart the server: `Ctrl+Shift+P` → **MCP: List Servers** → `dynatrace-managed-local` → **Restart** (do not fall back to direct terminal API calls).

If a direct API call is ever unavoidable, read the token into a variable first — never inline it in the command.
