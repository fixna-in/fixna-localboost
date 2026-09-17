# Fixna Platform

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

### Option A — without Docker (embedded PostgreSQL, everything mocked)

1. **Start the backend** (from the repository root):

   ```bash
   mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=local-nodocker
   ```

   - First start downloads embedded PostgreSQL 17 binaries once (~40 MB);
     later starts are fast and offline.
   - An embedded PostgreSQL starts automatically on a free random port; the
     `local-nodocker` profile excludes Redis entirely (nothing uses it; rate
     limiting is in-memory) so **no infrastructure is needed at all**.
   - Flyway applies all migrations (V1–V9) on startup; the schema is
     identical to production. Data lives in a temp directory and is **wiped
     on every restart** — a clean demo slate each time.
   - Verify: <http://localhost:8080/actuator/health> → `{"status":"UP"}`

2. **Create your first tenant account** (the demo seed has no users):

   - Swagger: <http://localhost:8080/swagger-ui.html> →
     `POST /api/v1/auth/register` with body
     `{"email":"owner@example.com","password":"password123","firstName":"Demo","lastName":"Owner","tenantName":"Demo Tenant"}`
   - Or curl:
     `curl -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d "{\"email\":\"owner@example.com\",\"password\":\"password123\",\"firstName\":\"Demo\",\"lastName\":\"Owner\",\"tenantName\":\"Demo Tenant\"}"`

3. **Start the frontend** (new terminal, from the repository root):

   ```bash
   cd frontend
   copy .env.example .env.local   # defaults point at http://localhost:8080/api
   npm install
   npm run dev
   ```

4. Open <http://localhost:3000> — the homepage health probe talks to the
   backend.

What is mocked in this mode: AI provider (`MockAIProvider`), advertising
adapters (deterministic Google/Meta/WhatsApp mocks), no Redis, no external
credentials anywhere. Registration = user + tenant + owner membership in one
call.

5. **Stop**: `Ctrl+C` on both terminals. All state is discarded
   (backend) — nothing to clean up.

### Option B — with Docker Compose (persistent local Postgres/Redis)

1. Copy `.env.example` to `.env` (optional; compose has matching local
   defaults) and `frontend/.env.example` to `frontend/.env.local`.
2. Start infrastructure: `docker compose up -d` (PostgreSQL 5432, Redis
   6379, Mailhog UI 8025).
3. Start the backend with the Docker-backed local profile:

   ```bash
   mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
   ```

   - API base: `http://localhost:8080/api/v1`
   - Swagger UI: <http://localhost:8080/swagger-ui.html>
   - Health: <http://localhost:8080/actuator/health>
4. Start the frontend as in Option A step 3 and open
   <http://localhost:3000>.
5. **Stop**: `Ctrl+C` on both terminals; `docker compose down` (add `-v` to
   wipe data volumes).

### Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `Port 8080 was already in use` | Another process (often an earlier dev run) holds 8080. Stop it or set `SERVER_PORT`. |
| `password authentication failed for user "fixna"` against localhost:5432 | You did NOT run with the `local-nodocker` profile — the app fell back to `localhost:5432` defaults. Run the exact command from Option A (profile required). |
| First backend start is slow | The one-time embedded PostgreSQL binary download. Subsequent starts are offline and fast. |
| Frontend shows CORS errors | `FIXNA_CORS_ALLOWED_ORIGINS` must include `http://localhost:3000` (defaults already do). |
| Schema-validation errors on startup | Flyway migrations must run first; check the startup log for `Successfully applied N migrations`. |
| Health shows `DOWN` in Option B | Docker services not running — `docker compose up -d` and re-check. |

### Testing

Backend (unit + integration suites run without Docker; the
`FixnaEndToEndTest` Testcontainers journey against real PostgreSQL
auto-skips when Docker is unavailable and runs in CI):

`mvn -f backend/pom.xml test`

Frontend:

`cd frontend && npm install && npm run build`

## CI Pack

## Fixna LocalBoost - GitHub Actions + Cline CI Pack

This pack adds a lightweight free-tier-friendly CI setup for the Fixna LocalBoost repository.

## Files

- `.github/workflows/ci.yml`
  - Java 21 / Maven compile
  - Java tests
  - Node.js 22
  - Next.js production build
  - Runs on pushes to `main`/`develop` and pull requests.
- `.clinerules/16-agent-execution.md`
  - Prevents Cline from looping on build logs.
  - Defines local validation and CI behavior.
- `docs/development/branch-strategy.md`
  - Defines main/develop/feature/fix branch usage.

## Important

The current Fixna frontend does not include a `package-lock.json`, so the workflow intentionally uses:

`npm install`

instead of:

`npm ci`

Once a lock file is committed, change the workflow to `npm ci` and set:

`cache-dependency-path: frontend/package-lock.json`

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


See `AGENTS.md` and `.clinerules/` before using an AI coding agent.

