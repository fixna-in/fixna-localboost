# Client Onboarding — Setup & Configuration

This document covers everything needed to onboard a **new deployment**, **development environment**, or **tenant workspace** onto Fixna LocalBoost.

---

## 1. Prerequisites

### For local development

| Component | Version | Notes |
|-----------|---------|-------|
| JDK | 21 (Temurin) | Required for backend |
| Maven | 3.9+ | Backend build and run |
| Node.js | 22 | Frontend build |
| npm | bundled with Node | Use `npm ci` in CI and for reproducible installs |
| PostgreSQL | 14+ | Persistent local profile uses `localhost:5432` |
| psql | optional | One-time schema/demo SQL load |

Docker is **optional**. E2E tests use Testcontainers when Docker is available; they skip locally otherwise.

### For production

- Managed PostgreSQL (non-localhost URL required on `prod` profile)
- TLS-terminated reverse proxy (HTTPS)
- Secret manager or secure env injection for credentials
- Optional: Redis, OTLP collector for traces

---

## 2. Environment profiles

Spring profiles control behaviour, logging format, and fail-fast guards.

| Profile | Purpose | OTLP export | Logging |
|---------|---------|-------------|---------|
| `local` | Developer machine + persistent PG | Off | Human-readable |
| `test` | Unit/integration tests | Off | Quiet test config |
| `dev` | Shared dev/staging host | On (configurable) | JSON structured |
| `staging` | Pre-production | On | JSON structured |
| `prod` | Production | On | JSON structured |

Activate via:

```bash
# Maven
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=local

# Environment
export SPRING_PROFILES_ACTIVE=local
```

### Profile files

| File | Role |
|------|------|
| `backend/src/main/resources/application.yml` | Base defaults |
| `application-local.yml` | `localboost` DB on localhost, Redis disabled |
| `application-dev.yml` | HSTS + OTLP defaults for shared dev |
| `application-staging.yml` | Staging overrides |
| `application-prod.yml` | No localhost fallbacks; strict secrets |
| `application-test.yml` | Test harness |

---

## 3. Configuration reference

### 3.1 Root `.env.example` (operator reference)

Copy to `.env` for local tooling — **never commit real secrets**.

| Variable | Default / example | Required | Description |
|----------|-------------------|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Yes | Active Spring profile |
| `SERVER_PORT` | `8080` | No | Backend HTTP port |
| `POSTGRES_DB` | `fixna` | Yes* | Database name |
| `POSTGRES_USER` | `fixna` | Yes* | DB user |
| `POSTGRES_PASSWORD` | — | Yes* | DB password (runtime only) |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/fixna` | Yes* | JDBC URL |
| `REDIS_URL` | `redis://localhost:6379` | No | Optional cache |
| `FIXNA_JWT_SECRET` | — | **Yes in prod** | Min 32 bytes; signs access tokens |
| `FIXNA_JWT_ACCESS_TTL` | `PT15M` | No | Access token lifetime |
| `FIXNA_JWT_REFRESH_TTL` | `P7D` | No | Refresh token lifetime |
| `FIXNA_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,...` | Yes if SPA | Comma-separated frontend origins |
| `FIXNA_AI_PROVIDER` | `mock` | No | `mock` until real provider wired |
| `FIXNA_AI_DAILY_QUOTA` | `50` | No | Per-tenant AI calls / UTC day |
| `FIXNA_PLATFORM_MODE` | `mock` | No | Advertising adapter mode |
| `FIXNA_APP_ENV` | `local` | No | Appears in structured logs |
| `OTEL_EXPORTER_OTLP_ENABLED` | `false` (local) | No | Trace export toggle |
| `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` | — | When OTLP on | Collector endpoint |

\* `local` profile uses `postgres` / `localboost` — see [Persistent local setup](#4-persistent-local-postgresql-setup).

### 3.2 Frontend `frontend/.env.example`

| Variable | Example | Notes |
|----------|---------|-------|
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080/api` | Must include `/api` prefix |
| `NEXT_PUBLIC_APP_ENV` | `local` | Display/diagnostics only |

**Never** put API keys, JWT secrets, or provider tokens in `NEXT_PUBLIC_*` variables.

### 3.3 Spring `fixna.*` namespace (application.yml)

| Key | Env override | Description |
|-----|--------------|-------------|
| `fixna.security.jwt.secret` | `FIXNA_JWT_SECRET` | JWT signing key |
| `fixna.security.cors.allowed-origins` | `FIXNA_CORS_ALLOWED_ORIGINS` | CORS allowlist |
| `fixna.security.rate-limit.per-ip-per-minute` | `FIXNA_RATE_LIMIT_PER_IP_PER_MINUTE` | Auth endpoint throttle (default 20) |
| `fixna.security.hsts-enabled` | `FIXNA_HSTS_ENABLED` | HSTS response header |
| `fixna.ai.provider` | `FIXNA_AI_PROVIDER` | AI provider id |
| `fixna.ai.daily-quota` | `FIXNA_AI_DAILY_QUOTA` | Tenant AI quota |
| `fixna.platform.default-mode` | `FIXNA_PLATFORM_MODE` | `mock` for demo |
| `fixna.billing.default-plan` | `FIXNA_BILLING_DEFAULT_PLAN` | Lazy subscription default |

### 3.4 Production fail-fast rules

On `prod` profile, startup **refuses** to run if:

| Check | Validator | Failure |
|-------|-----------|---------|
| JWT secret missing, placeholder, or &lt; 32 bytes | `SecurityStartupValidator` | Process exit |
| DB URL is localhost or default | `ProdEnvironmentValidator` | Process exit |
| DB password missing or `change-me` | `ProdEnvironmentValidator` | Process exit |

Redis missing only logs a warning (MVP).

---

## 4. Persistent local PostgreSQL setup

### 4.1 One-time database creation

```powershell
psql -X -h localhost -U postgres -d postgres -c 'CREATE DATABASE localboost'
psql -X -h localhost -U postgres -d localboost -f 'C:\Users\Dell\workspace\fixna-localboost\tools\sql\schema.sql'
```

`schema.sql` applies V1–V9 schema and Flyway baseline. **Do not** run on an already-migrated database.

### 4.2 One-time demo data (optional)

```powershell
$env:FIXNA_TEST_PASSWORD_HASH = Read-Host 'Paste BCrypt hash of demo password'
try {
    psql -X -h localhost -U postgres -d localboost -f 'C:\Users\Dell\workspace\fixna-localboost\tools\sql\demo-data.sql'
} finally {
    Remove-Item Env:FIXNA_TEST_PASSWORD_HASH
}
```

Fixture includes `owner@example.com` (TENANT_OWNER), cafe business, campaigns, leads, metrics.

### 4.3 Everyday startup

```powershell
& 'C:\Users\Dell\workspace\fixna-localboost\tools\start-local-demo.cmd'
```

Prompts for `POSTGRES_PASSWORD`, starts backend (`local` profile), runs `npm ci`, starts frontend.

Options: `-BackendOnly`, `-BackendPort`, `-FrontendPort`, `-SmokeTest`, `-TimeoutSeconds`.

### 4.4 Embedded demo (no PostgreSQL install)

The launcher can use a temporary embedded database for smoke demos. Credentials are shown once in the terminal — not saved to disk.

---

## 5. Build and validation

```bash
# Backend — all tests
mvn -f backend/pom.xml test

# Frontend — production build
cd frontend && npm ci && npm run build
```

CI (`.github/workflows/ci.yml`): Java 21 tests + Node 22 `npm ci` + `npm run build`.

---

## 6. Tenant user onboarding (application)

After the platform is running, each **client organization** (tenant) follows this sequence.

### Step 1 — Register

| UI | API |
|----|-----|
| `/register` | `POST /api/v1/auth/register` |

Payload: `email`, `password` (≥ 8 chars), `firstName`, `lastName`, `tenantName`.

Result: user + tenant created; caller becomes **TENANT_OWNER**; JWT returned.

### Step 2 — Sign in

| UI | API |
|----|-----|
| `/login` | `POST /api/v1/auth/login` |

Tenant is selected **server-side** from memberships (not from client input).

### Step 3 — Create business

| UI | API |
|----|-----|
| `/businesses` | `POST /api/v1/businesses` |

Fields: `name`, `category`, `description`, `websiteUrl`, `phone`.

Plan limit: FREE = 1 business (see [Plans](#8-subscription-plans-and-limits)).

### Step 4 — Add location (optional)

| UI | API |
|----|-----|
| `/businesses/{id}` | `POST /api/v1/businesses/{id}/locations` |

Fields: address, city, state, postal code, country (default India), lat/long.

### Step 5 — Connect ad platforms (mock)

| API | Notes |
|-----|-------|
| `POST /api/v1/platform-connections` | `platform`: `GOOGLE`, `META`, or `WHATSAPP` |

Mock mode: no credentials stored. Real provider integration is Phase 7.

### Step 6 — Create first campaign

See [Business flows](./business-flows.md#4-campaigns-lifecycle).

### Step 7 — Dashboard

| UI | API |
|----|-----|
| `/dashboard` | `GET /api/v1/analytics/dashboard` |

---

## 7. Roles and permissions

| Role | Read | Write (business/campaign) | Administer members |
|------|------|---------------------------|-------------------|
| TENANT_OWNER | ✓ | ✓ | ✓ |
| TENANT_ADMIN | ✓ | ✓ | ✓ |
| TENANT_MARKETING_MANAGER | ✓ | ✓ | ✗ |
| TENANT_MARKETING_USER | ✓ | ✓ | ✗ |
| TENANT_VIEWER | ✓ | ✗ | ✗ |
| AGENCY_ADMIN | ✓ | ✓ | ✓ |
| AGENCY_USER | ✓ | ✓ | ✗ |

Platform admin (`INTERNAL` tenant) uses `/api/v1/admin/*` — not a membership role.

---

## 8. Subscription plans and limits

| Plan | Max businesses | Max campaigns | AI requests / day |
|------|----------------|---------------|-------------------|
| FREE | 1 | 1 | 10 |
| STARTER | 3 | 5 | 100 |
| GROWTH | 10 | 20 | 1000 |

- Current plan: `GET /api/v1/subscriptions/current`
- Platform admin plan change: `PUT /api/v1/admin/tenants/{tenantId}/plan`
- No payment provider in MVP

Error codes when over limit: `PLAN_BUSINESS_LIMIT`, `PLAN_CAMPAIGN_LIMIT`, `AI_QUOTA_EXCEEDED`.

---

## 9. Observability configuration

| Endpoint | Purpose |
|----------|---------|
| `GET /api/v1/health` | Liveness (public) |
| `GET /api/v1/health/readiness` | Readiness probe |
| `GET /actuator/health` | Spring Actuator |
| `GET /actuator/prometheus` | Metrics |

Structured logs (dev/staging/prod): JSON on stdout with `traceId`, `spanId`, `requestId`, `tenantId`, `userId`, `campaignId`, `operation`.

Log config: `backend/src/main/resources/log4j2-spring.xml`.

---

## 10. Security checklist for go-live

- [ ] Generate strong `FIXNA_JWT_SECRET` (≥ 32 bytes, not the dev placeholder)
- [ ] Set production PostgreSQL URL and credentials via secret manager
- [ ] Configure `FIXNA_CORS_ALLOWED_ORIGINS` to exact frontend origin(s)
- [ ] Enable HTTPS and `FIXNA_HSTS_ENABLED=true`
- [ ] Set `FIXNA_AI_PROVIDER` and provider keys only on the server (never in frontend)
- [ ] Confirm `prod` profile starts without warnings from `SecurityStartupValidator`
- [ ] Review rate limits on `/api/v1/auth/**`
- [ ] Ensure audit logging destination is configured for your log pipeline

---

## 11. Troubleshooting

| Symptom | Fix |
|---------|-----|
| Port 8080 in use | Stop other process or set `SERVER_PORT` |
| PostgreSQL auth failed | Check `POSTGRES_PASSWORD` with `local` profile |
| CORS errors in browser | Add frontend origin to `FIXNA_CORS_ALLOWED_ORIGINS` |
| Flyway validation error | Ensure migrations ran; check startup log |
| `FORBIDDEN` on writes | User may be TENANT_VIEWER |
| `AI_QUOTA_EXCEEDED` | Wait for UTC day rollover or upgrade plan |
| `INVALID_PLAN_CODE` (admin) | Use `FREE`, `STARTER`, or `GROWTH` |

More: `docs/07-operations/troubleshooting.md`, [README](../../README.md).
