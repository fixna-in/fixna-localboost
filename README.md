# Fixna Platform

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

## Architecture
- Frontend: Next.js + React + TypeScript
- Backend: Java 21 + Spring Boot 3.x
- Database: PostgreSQL + Flyway
- Cache: Redis
- AI: provider abstraction with mock provider for local development
- External advertising: adapter pattern for Google Ads, Meta Ads and WhatsApp
- Tenancy: shared PostgreSQL schema with mandatory `tenant_id`, designed for future RLS

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
| Database health is DOWN | Check the local PostgreSQL service and database credentials. |

### Testing

Backend (unit + integration suites run without Docker; the
`FixnaEndToEndTest` Testcontainers journey against real PostgreSQL
auto-skips when Docker is unavailable and runs in CI):

`mvn -f backend/pom.xml test`

Frontend:

`cd frontend && npm install && npm run build`

## CI Pack

## Fixna LocalBoost - GitHub Actions + Cursor CI Pack

This pack adds a lightweight free-tier-friendly CI setup for the Fixna LocalBoost repository.

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

## Future CI stages

Add these only after the basic pipeline is stable:

- Checkstyle/SpotBugs
- Test coverage
- PostgreSQL integration tests
- Docker image build
- Container image security scanning
- Deployment to a VPS/cloud environment


See `AGENTS.md` and `.cursor/rules/` before using an AI coding agent.

## Onboarding documentation

- [Onboarding hub](docs/00-product/ONBOARDING.md) — start here
- [Client setup & configuration](docs/00-product/client-onboarding.md) — environments, env vars, first-run
- [Business flows](docs/00-product/business-flows.md) — every feature, step-by-step

