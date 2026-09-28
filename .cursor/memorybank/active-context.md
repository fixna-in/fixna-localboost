# Active Context — Fixna LocalBoost

**Last updated:** 2026-09-28  
**Release:** **1.0.1** — shared demo fully operational

## Current focus

**v1.0.1 shipped.** Demo on **fixna.in** is live end-to-end: Neon + Render API +
Vercel frontend, DNS, health API, GitHub org **fixna-in**, CI green.

| App | URL |
|-----|-----|
| Dashboard | https://app.fixna.in/dashboard |
| Campaigns | https://app.fixna.in/campaigns |
| API health | https://api.fixna.in/api/v1/health |
| Actuator | https://api.fixna.in/actuator/health |

## Live stack

| Layer | Provider | Notes |
|-------|----------|-------|
| Source | GitHub **fixna-in/fixna-localboost** | Org repo; Render + Vercel connected |
| Database | Neon PostgreSQL (`fixna`) | Manual SQL seed only |
| API | Render (`fixna-localboost-api`) | `staging` profile, Docker |
| Frontend | Vercel (`frontend/`) | Next.js **16.3.6** |

## Recent fixes (1.0.1)

- Enhanced `/api/v1/health` (components + version + `deployedAt`)
- Removed `FlywayHealthIndicator` bean conflict (Render startup)
- Next.js 16.3.6; CI dependency-review on PRs only
- Local launcher: `mvn clean compile` before backend start
- Flyway 11.20 for PostgreSQL 18

## Brand mark (unified)

Canonical **`f•`** on `#163e32` with lime dot `#c7ed94` — see
`frontend/src/brand/brand-mark-graphic.tsx`.

## Demo data (Neon — manual)

1. `tools/sql/neon-demo-seed.sql` — `owner@example.com`, tenant, business
2. `tools/sql/neon-demo-data.sql` — campaigns, leads, metrics

Password: `tools\password-tool.cmd hash` → SQL `UPDATE users SET password_hash = ...`

## Key env vars

**Render:** `SPRING_PROFILES_ACTIVE`, `SPRING_DATASOURCE_URL`, `POSTGRES_*`,
`FIXNA_JWT_SECRET`, `FIXNA_CORS_ALLOWED_ORIGINS`, optional `FIXNA_DEPLOYED_AT`

**Vercel:** `NEXT_PUBLIC_API_BASE_URL=https://api.fixna.in/api`, `NEXT_PUBLIC_APP_ENV=demo`

## Agent handoff

Full no-secrets brief: `.cursor/memorybank/chatgpt-handoff.md`

## Next recommended work

- [ ] E2E smoke on production URLs (login → dashboard → campaigns)
- [ ] Vitest/Playwright frontend tests
- [ ] OpenAPI generation from springdoc
- [ ] Optional: Vercel Pro or CLI deploy if org repo stays private on Hobby
