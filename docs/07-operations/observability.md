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

## Correlation

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
| Liveness                 | `GET /api/v1/health`          | 200 `UP` — process is serving             |
| Readiness                | `GET /api/v1/health/readiness`| 200 `READY` only after `ApplicationReadyEvent`; 503 `DOWN` before |
| Component health         | `GET /actuator/health`        | DB/Redis/disk checks                       |
| OpenAPI                  | `GET /v3/api-docs`            | API surface                                |

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

## OpenTelemetry readiness

- All correlation access goes through `TelemetryContext` and all timing
  through `OperationTimer` — single seams to swap for OTel `Tracer` +
  `Span` without touching call sites.
- Structured single-line `key=value` log records map 1:1 to OTel log
  attributes, enabling a centralized JSON/OTLP exporter later.
- Log levels are profile-aware (`logging.level.*` in the common
  `application.yml`; environment overrides live in profile files).

## Secrets policy

Never place secrets in logs, error bodies, query-echoing paths, cache keys
or metrics. `GlobalExceptionHandler` logs unexpected Throwables with
correlation ids only and returns the generic `INTERNAL_ERROR` envelope.
