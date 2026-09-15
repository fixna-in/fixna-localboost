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

## Local development
1. Copy `.env.example` to `.env`
2. Start infrastructure with `docker compose up -d`
3. Start backend and frontend using their project instructions.
4. Demo mode works without external advertising or LLM credentials.

# Fixna LocalBoost - GitHub Actions + Cline CI Pack

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

