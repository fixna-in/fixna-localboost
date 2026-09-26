# Observability

Fixna LocalBoost observability foundations: structured logging, request
correlation, health/readiness probes, application metrics and business
operation telemetry. Everything here is OpenTelemetry-ready — hooks are
centralized so a real OTel tracer can be swapped in later without touching
business code.

## Logging conventions (rules 14)

- SLF4J only. No `System.out` / `System.err` in production code.
- Parameterized logging (`log.info("x={}", x)`) — never string concatenation.
- Log exception objects to preserve stack traces.
- Never log credentials, OAuth tokens, passwords, Authorization headers,
  cookies, SDK/API keys, or raw sensitive payloads.
- Never log full request/response bodies by default.
- Keep tenant/user/business data to ids, not names/PII, unless the audit
  trail explicitly requires it.

### Logger categories

| Logger           | Purpose                                                     |
|------------------|-------------------------------------------------------------|
| `fixna.access`   | One line per API request (method/path/status/durationMs)    |
| `fixna.audit`    | Security-relevant business facts (also persisted in DB)     |
| `fixna.telemetry`| Operation timings (AI, platform adapters)                   |
| `fixna.error`    | Unexpected Throwables, server-side only                     |
| root / in.fixna.platform | Component diagnostics                                  |

### Levels

- `ERROR` — failed operation requiring attention
- `WARN` — unusual/recoverable condition
- `INFO` — important lifecycle/business event (access lines, telemetry)
- `DEBUG`/`TRACE` — developer diagnostics (local/profiles only)

### Access log format

One structured line per `/api/v1/**` request (URI only — never query strings,
never headers/bodies). Emitted by `RequestLoggingFilter` after the response
is complete:

```text
method=POST path=/api/v1/campaigns/123/launch status=201 durationMs=84 requestId=abc tenantId=123 uuid
```

## Log4j2 (logging implementation)

SLF4J is the API; **Log4j2 is the implementation**. `spring-boot-starter-log4j2`
is on the classpath and `spring-boot-starter-logging` (Logback) is excluded from
every starter in `backend/pom.xml`, so there is exactly one SLF4J binding.

- Configuration: `backend/src/main/resources/log4j2-spring.xml`
  (Spring-Boot-aware, so profile blocks and `logging.level.*` overrides work).
- Test configuration: `backend/src/test/resources/log4j2-test.xml`
  (quiet pattern, no file appenders, no JSON).
- Application helper: `in.fixna.platform.common.logging.LoggingContext`
  (MDC population/clearing) and `LoggingConstants` (MDC key + operation names).
- Sensitive-value guard: `in.fixna.platform.common.logging.SensitiveDataMasker`.

### Environment behaviour

| Profiles            | Appender | Format                                              |
|---------------------|----------|-----------------------------------------------------|
| `local`, `test`, default | Console (stdout) | human-readable pattern, truncated stack traces |
| `dev`, `staging`, `prod` | Console (stdout) | JSON (`JsonLayout`) for ELK/OpenSearch/Loki/Datadog/CloudWatch/Azure Monitor |

Container-native: nothing is written to disk — log volume/file rotation stays a
platform (Docker/Kubernetes) concern.

Local / test pattern:

```text
2026-09-18 10:15:32.123 INFO  [traceId=abc spanId=def requestId=req-1 tenantId=t-1 userId=u-1 campaignId=c-1 operation=CAMPAIGN_CREATE] CampaignService - Campaign created successfully campaignId=c-1 tenantId=t-1
```

JSON records carry `timestamp`, `level`, `loggerName`, `message`, `threadName`,
`service`, `environment`, plus every MDC key present on the event (only keys that
are set are emitted, so no `null` noise).

### Service and environment identity

- `service` = `spring.application.name` (`fixna-localboost-backend` default).
- `environment` = `fixna.app-env` / `FIXNA_APP_ENV` (default `unknown`).
  Profile files set it to `local`, `dev`, `staging`, `prod`; nothing
  production-specific is hardcoded in the Log4j2 configuration.

### Per-environment log levels

`application.yml` sets the baseline (`logging.level.root=INFO`,
`logging.level.in.fixna.platform=INFO`); profile files override it —
`local`/`dev` raise `in.fixna.platform` to `DEBUG`, `prod` keeps `INFO` with
root at `WARN`-quiet third parties. Overrides use Spring Boot's standard
`logging.level.*` properties, which Log4j2 honours through `log4j2-spring.xml`.

## Correlation

- MDC keys: `traceId`, `spanId`, `requestId`, `tenantId`, `userId`,
  `campaignId`, `operation` — the same names are emitted as JSON fields.
- `X-Request-Id`: accepted when safe or minted (UUID) by `RequestIdFilter`,
  returned on the response, placed in SLF4J MDC and the servlet request
  attribute.
- `TelemetryContext` resolves `requestId`/`tenantId`/`userId` from MDC and the
  JWT-derived `TenantContext` — never from client input.
- The `ApiError` envelope echoes `requestId` so clients can correlate failures.
- When a real OpenTelemetry tracer is added, prefer trace/span ids; keep the
  `X-Request-Id` as the human/API-facing correlation key.

## Health and readiness

| Probe                    | Endpoint                      | Semantics                                  |
|--------------------------|-------------------------------|--------------------------------------------|
| Aggregated health        | `GET /api/v1/health`          | 200 when overall status is `UP`; 503 otherwise. Returns accumulated status, per-component probes, `version`, `deployedAt`, `environment`, `service`, and `timestamp`. |
| Readiness                | `GET /api/v1/health/readiness`| 200 `READY` only after `ApplicationReadyEvent`; 503 `DOWN` before |
| Actuator health          | `GET /actuator/health`        | Same component contributors as the public API (`db`, `diskSpace`, `ping`, `flyway`, `platform`, …) with details enabled |
| OpenAPI                  | `GET /v3/api-docs`            | API surface                                |

`GET /api/v1/health` example (trimmed):

```json
{
  "status": "UP",
  "version": "1.0.0",
  "deployedAt": "2026-09-26T12:00:00Z",
  "environment": "demo",
  "service": "fixna-localboost-backend",
  "timestamp": "2026-09-26T18:30:00+05:30",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL" } },
    "flyway": { "status": "UP", "details": { "applied": 9, "pending": 0, "currentVersion": "9" } },
    "platform": { "status": "UP", "details": { "version": "1.0.0", "environment": "demo", "deployedAt": "..." } }
  }
}
```

- **version** — Maven `build-info` (`spring-boot-maven-plugin` `build-info` goal).
- **deployedAt** — `FIXNA_DEPLOYED_AT` env var when set; otherwise build-info timestamp.

`AppReadiness` flips the readiness gate only after the Spring context is fully
initialized, so load balancers never route traffic into a half-started app.

## Metrics

- Actuator endpoints exposed: `health`, `info`, `metrics`, `prometheus`
  (`management.endpoints.web.exposure.include` in `application.yml`).
- Micrometer-backed counters/timers are available for business gauges; today
  the primary operational numbers are the access-log lines and operation
  telemetry below.

## Operation telemetry (business timings)

`OperationTimer` (AutoCloseable) times a block and emits one line to
`fixna.telemetry`:

```text
operation=ai.recommend status=SUCCESS durationMs=240 requestId=abc tenantId=123 entity=campaign:456
operation=platform.launch status=SUCCESS durationMs=12 requestId=abc tenantId=123 entity=campaign:456 platform=GOOGLE
```

Currently instrumented:

- `ai.recommend` — each AI recommendation provider call
  (`AiRecommendationService`), including `PROVIDER_ERROR` states.
- `platform.launch` — each adapter launch attempt, per attempt, with the
  adapter's platform (`CampaignLaunchService`), statuses `SUCCESS`/`FAILED`.

`TelemetryContext` resolves correlation ids; the timer never logs payloads,
prompts, responses or tokens. AI usage/cost itself is persisted in
`ai_usage_log` (rules 11/13).

## OpenTelemetry

Dependencies: `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`.

| Profile   | OTLP export | Notes |
|-----------|-------------|-------|
| `local`   | off         | Tracing may run; spans are not exported |
| `test`    | off         | Keeps suites infrastructure-free |
| `dev`/`staging`/`prod` | on (default) | Endpoint via `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` |

Base config (`application.yml`):

- `management.tracing.sampling.probability` (default `1.0`)
- `management.otlp.tracing.export.enabled` (default `false`)
- `management.otlp.tracing.endpoint` (empty until set)

`RequestIdFilter` copies the active Micrometer span's `traceId`/`spanId` into
MDC (`LoggingContext.putTrace`) so JSON and human-readable logs correlate with
exported traces. `X-Request-Id` remains the API-facing correlation key.

Operation timings still flow through `OperationTimer` → `fixna.telemetry`; a
future step can emit OTel span events from that seam without touching call sites.

## Secrets policy

Never place secrets in logs, error bodies, query-echoing paths, cache keys
or metrics. `GlobalExceptionHandler` logs unexpected Throwables with
correlation ids only and returns the generic `INTERNAL_ERROR` envelope.
