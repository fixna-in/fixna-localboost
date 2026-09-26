# Fixna Platform

**Fixna LocalBoost v1.0.0** — [app.fixna.in](https://app.fixna.in) · [api.fixna.in](https://api.fixna.in)

## One-command local demo (Windows)

Run `C:\Users\Dell\workspace\fixna-localboost\tools\start-local-demo.cmd`
from CMD or PowerShell. Requires Java 21, Maven, Node.js and npm on PATH;
first startup needs network access for dependencies. No Docker is needed.
The CMD wrapper uses a process-scoped PowerShell execution-policy override,
not a permanent system setting.

The launcher starts the backend, seeds a new temporary database, verifies
login and data through the API, runs `npm ci`, and starts the frontend. Only
then does it display the login URL, email and a freshly generated password.
Credentials are not saved to files. Keep the window open; Ctrl+C stops the
processes started by the launcher. Existing servers are never reused or killed.
Do not run it concurrently with another frontend using the same checkout.

Options (append to the CMD command):
- `-BackendPort 18081 -FrontendPort 3001`: alternate ports (defaults 8080/3000).
- `-BackendOnly`: omit npm/frontend startup.
- `-SmokeTest -BackendOnly -BackendPort 18081`: verify APIs and stop automatically;
  suppresses password display. Nonzero exit means startup/verification failed.
- `-TimeoutSeconds 600`: allow longer dependency downloads (default 300 seconds).

Fixture: `owner@example.com` with TENANT_OWNER workspace administration, one
SMB tenant/company and Noida cafe location, a STARTER demo subscription, two
campaigns (DRAFT and synthetic COMPLETED), offers, channel allocations, radius
targeting, audiences, four draft creatives, ten fictional leads covering all
pipeline stages, and seven days of sample metrics. This is not PLATFORM_ADMIN
access. No payment, provider credentials, real campaign execution, or fabricated
AI recommendation is created. Generate advisory recommendations through the UI.
Existing owner emails skip the entire SQL fixture, preserving prior edits.
Data/password reset on each launcher run because the embedded DB is temporary.
Logs are stored in a unique `fixna-demo-*` directory in the OS temp folder.


Fixna is the platform brand. **Fixna LocalBoost** is the first product: an AI-assisted,
multi-tenant local advertising orchestration platform for SMBs.

## Domains
- `https://fixna.in` — brand/marketing site
- `https://app.fixna.in` — application
- `https://api.fixna.in` — API
- `https://admin.fixna.in` — administration (future)
- `https://docs.fixna.in` — documentation (future)

## Shared demo (live)

| Component | URL |
|-----------|-----|
| Frontend | [app.fixna.in](https://app.fixna.in) |
| API | [api.fixna.in](https://api.fixna.in) |
| Health | [api.fixna.in/api/v1/health](https://api.fixna.in/api/v1/health) |

**Stack:** Neon PostgreSQL + Render API (Docker) + Vercel frontend — free-tier
friendly, no card required. Full setup:
[docs/07-operations/deployment.md](docs/07-operations/deployment.md).

**Neon demo data** (manual, not on app startup): run
`tools/sql/neon-demo-seed.sql` then `tools/sql/neon-demo-data.sql` in the Neon SQL
Editor after Flyway migrations. Do not use `tools/sql/demo-data.sql` on Neon (psql-only,
for local `localboost`).

**Render env templates:** `infrastructure/demo/render.env.example`,
`infrastructure/demo/vercel.env.example`.

### Health and observability

`GET /api/v1/health` (public) returns aggregated platform health:

- **status** — overall UP/DOWN (HTTP 200 vs 503)
- **components** — per-probe status and details (`db`, `diskSpace`, `ping`, `flyway`, `platform`, …)
- **version** — from Maven `build-info` (currently **1.0.0**)
- **deployedAt** — `FIXNA_DEPLOYED_AT` on Render when set; otherwise image build time
- **environment** — `FIXNA_APP_ENV` (e.g. `demo` on Vercel)
- **service** — `spring.application.name`
- **timestamp** — snapshot time

Example (trimmed):

```json
{
  "status": "UP",
  "version": "1.0.0",
  "deployedAt": "2026-09-26T12:00:00Z",
  "environment": "demo",
  "service": "fixna-localboost-backend",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL" } },
    "flyway": { "status": "UP", "details": { "applied": 9, "pending": 0 } },
    "platform": { "status": "UP", "details": { "version": "1.0.0", "environment": "demo" } }
  }
}
```

| Endpoint | Purpose |
|----------|---------|
| `GET /api/v1/health` | Aggregated health + version + deploy metadata |
| `GET /api/v1/health/readiness` | Readiness (503 until Spring context is ready) |
| `GET /actuator/health` | Actuator component health (details enabled) |
| `GET /actuator/prometheus` | Metrics |

More: [docs/07-operations/observability.md](docs/07-operations/observability.md).

## Architecture
- Frontend: Next.js 16 + React + TypeScript ([app.fixna.in](https://app.fixna.in))
- Backend: Java 21 + Spring Boot 3.5 ([api.fixna.in](https://api.fixna.in))
- Database: PostgreSQL + Flyway (V1–V9)
- Cache: Redis (local/dev; disabled on staging demo)
- AI: provider abstraction with mock provider for local and demo
- External advertising: adapter pattern for Google Ads, Meta Ads and WhatsApp (mock on demo)
- Tenancy: shared PostgreSQL schema with mandatory `tenant_id`, designed for future RLS
- Observability: Log4j2 structured logs, `X-Request-Id` correlation, Micrometer metrics,
  aggregated `/api/v1/health` with component probes and release metadata

## Run locally

Demo mode works without external advertising or LLM credentials — the AI
provider and all ad-platform adapters are deterministic mocks by default.
Two supported paths: **without Docker** (recommended, everything mocked and
embedded) or with Docker Compose.

### Prerequisites

- JDK 21 (Temurin), Maven 3.9+
- Node.js 22 + npm
- Docker Desktop — only for Option B

### Persistent local PostgreSQL: one-time setup, then one launcher

PostgreSQL must already be running on `localhost:5432`. Use database `localboost`
with user `postgres` for local development only. Passwords are supplied at runtime,
not saved in configuration. Existing `localboost` data does not need reloading.

**One-time setup for a NEW database** (psql must be on PATH):

```powershell
# Only if the database does not exist. Enter the PostgreSQL password when prompted.
psql -X -h localhost -U postgres -d postgres -c 'CREATE DATABASE localboost'
psql -X -h localhost -U postgres -d localboost -f 'C:\Users\Dell\workspace\fixna-localboost\tools\sql\schema.sql'
```

`schema.sql` contains the full V1–V9 schema and records a Flyway V9 baseline.
It runs atomically and refuses an existing public schema. Do not run it on your
already-migrated database or in production. Keep the original migrations immutable;
subsequent backend releases apply newer Flyway migrations normally.

**Load initial demo data once:** set `FIXNA_TEST_PASSWORD_HASH` to a BCrypt hash
of your chosen demo password (8–72 UTF-8 bytes). Supply a hash from a trusted local
BCrypt tool, never a plaintext password or an online password-hashing service.

```powershell
$env:FIXNA_TEST_PASSWORD_HASH = Read-Host 'Paste the BCrypt hash of your demo password'
try {
    psql -X -h localhost -U postgres -d localboost -f 'C:\Users\Dell\workspace\fixna-localboost\tools\sql\demo-data.sql'
} finally {
    Remove-Item Env:FIXNA_TEST_PASSWORD_HASH
}
```

`demo-data.sql` is standalone psql SQL (no includes), with one atomic insert-if-absent
fixture: tenant owner `owner@example.com`, workspace, cafe/location, subscription,
two campaigns, offers/channels/targeting/creatives, ten leads and seven metric rows.
Existing accounts and passwords are preserved; partial fixtures are not repaired.
The account is TENANT_OWNER, not PLATFORM_ADMIN. No real advertising occurs.

**Everyday startup — one command:**

```powershell
& 'C:\Users\Dell\workspace\fixna-localboost\tools\start-local-demo.cmd'
```

The `.cmd` is a thin entry point for the single PowerShell launcher. It asks only
for the database password, starts the backend with `local`, runs `npm ci`, starts
the frontend, and checks HTTP readiness. It does not create databases, load demo
SQL or reset passwords. Flyway still validates/applies versioned schema migrations.
Log in at http://localhost:3000/login with your existing account and password.
Keep the window open; Ctrl+C stops only its processes, not PostgreSQL.
Use `-BackendOnly`, `-BackendPort`, `-FrontendPort`, or `-SmokeTest` as needed.

Developer checks: `tools/test-local-demo-process.ps1` verifies process handling;
`tools/test-local-sql.ps1` uses a disposable local database and requires `PGPASSWORD`.
Only application-local.yml is supported for local development. It connects to localhost:5432/localboost as postgres, using POSTGRES_PASSWORD supplied at runtime. Redis and embedded PostgreSQL are not required. Data persists; startup does not load demo fixtures.

For backend-only startup, set POSTGRES_PASSWORD securely in your terminal and run Maven with -Dspring-boot.run.profiles=local. The one-command launcher above prompts for the password automatically.

### Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `Port 8080 was already in use` | Another process (often an earlier dev run) holds 8080. Stop it or set `SERVER_PORT`. |
| PostgreSQL authentication fails | Use the local profile and supply POSTGRES_PASSWORD for postgres on localhost:5432/localboost. |
| Frontend shows CORS errors | `FIXNA_CORS_ALLOWED_ORIGINS` must include `http://localhost:3000` (defaults already do). |
| Schema-validation errors on startup | Flyway migrations must run first; check the startup log for `Successfully applied N migrations`. |
| Database health is DOWN | Check the local PostgreSQL service and credentials; inspect `GET /api/v1/health` → `components.db`. |
| Shared demo health 503 | Verify Neon connectivity, Flyway migrations applied (`components.flyway`), and Render logs for `staging` profile. |

### Testing

Backend (unit + integration suites run without Docker; the
`FixnaEndToEndTest` Testcontainers journey against real PostgreSQL
auto-skips when Docker is unavailable and runs in CI):

`mvn -f backend/pom.xml test`

Frontend:

`cd frontend && npm install && npm run build`

## CI Pack

Lightweight free-tier-friendly CI for Fixna LocalBoost.

## Files

- `.github/workflows/ci.yml`
  - Java 21 / Maven compile
  - Java tests
  - Node.js 22
  - Next.js production build
  - Runs on pushes to `main`/`develop` and pull requests.
- `.cursor/rules/agent-execution.mdc`
  - Prevents the agent from looping on build logs.
  - Defines local validation and CI behavior.
- `docs/development/branch-strategy.md`
  - Defines main/develop/feature/fix branch usage.

## Important

The frontend workflow uses `npm ci` with `frontend/package-lock.json` for
reproducible installs and npm cache warming.

## Recommended first setup

1. Create a GitHub repository.
2. Put the Fixna LocalBoost project in that repository.
3. Copy these files into the corresponding paths.
4. Push to `develop` or `main`.
5. Open the GitHub Actions tab and verify the `Fixna LocalBoost CI` workflow.
6. For feature work, create a feature branch and open a pull request.

## Local validation

Backend:

`mvn -f backend/pom.xml test`

Frontend:

`cd frontend`
`npm install`
`npm run build`

Full milestone validation should pass both before merging.

## CI/CD

| Workflow | Purpose |
|----------|---------|
| `.github/workflows/ci.yml` | Test backend + build frontend on PR/push |
| Render (`render.yaml` + dashboard) | Auto-deploy API on push to `main` (free, no card) |
| Vercel (connect in dashboard) | Auto-deploy frontend on push to `main` |

After deploy, smoke-test `https://api.fixna.in/api/v1/health` (status `UP`, version
`1.0.0`, components populated) and log in at [app.fixna.in](https://app.fixna.in).

Setup: [docs/07-operations/deployment.md](docs/07-operations/deployment.md)

## Future CI stages

- Checkstyle/SpotBugs
- Test coverage
- Branch protection (CI must pass before deploy)
- Container image security scanning


See `AGENTS.md` and `.cursor/rules/` before using an AI coding agent.

## Onboarding documentation

- [Onboarding hub](docs/00-product/ONBOARDING.md) — start here
- [Client setup & configuration](docs/00-product/client-onboarding.md) — environments, env vars, first-run
- [Business flows](docs/00-product/business-flows.md) — every feature, step-by-step
- [Deployment (Neon + Render + Vercel)](docs/07-operations/deployment.md) — shared demo on fixna.in
- [Observability & health](docs/07-operations/observability.md) — probes, logs, metrics

