# Active Context — Fixna LocalBoost

**Last updated:** 2026-09-26

## Current focus

Shared demo deployment on **fixna.in** is **live** (API healthy). Frontend on Vercel
and custom DNS (`api.fixna.in`, `app.fixna.in`) are the remaining operator steps.

## Live stack (demo)

| Layer | Provider | Notes |
|-------|----------|-------|
| Database | Neon PostgreSQL (`fixna`) | Flyway on API startup; manual SQL seed |
| API | Render (Docker, free tier, Singapore) | `SPRING_PROFILES_ACTIVE=staging` baked in Dockerfile |
| Frontend | Vercel | Root dir `frontend`, Next.js 15 |

## URLs

| Purpose | URL |
|---------|-----|
| API (Render default) | `https://fixna-localboost.onrender.com` |
| API health | `https://fixna-localboost.onrender.com/actuator/health` |
| API (target custom) | `https://api.fixna.in` |
| App (target custom) | `https://app.fixna.in` |

## Demo data (Neon — manual, not on app startup)

1. `tools/sql/neon-demo-seed.sql` — user `owner@example.com`, tenant, business, subscription
2. `tools/sql/neon-demo-data.sql` — location, campaigns, leads, metrics (idempotent)

Password hash: `tools\password-tool.cmd hash` → `UPDATE users SET password_hash = ...`

## Staging profile highlights

- `fixna.test-data.enabled: false` — no LocalTestDataSeeder on Render
- Redis autoconfig excluded; `management.health.redis.enabled: false`
- `UserDetailsServiceAutoConfiguration` excluded — no generated Spring security password
- Mock AI + mock platform adapters

## Key env vars (Render)

- `SPRING_PROFILES_ACTIVE=staging` (Dockerfile default + dashboard)
- `SPRING_DATASOURCE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `FIXNA_JWT_SECRET` (48+ chars)
- `FIXNA_CORS_ALLOWED_ORIGINS` — must include exact frontend origin(s)

## Key env vars (Vercel)

- `NEXT_PUBLIC_API_BASE_URL` — must end with `/api` (e.g. `https://api.fixna.in/api`)
- `NEXT_PUBLIC_APP_ENV=demo`

## Repo cleanup (2026-09-26)

Removed abandoned Fly.io artifacts (`fly.toml`, deploy workflow, fly secrets template),
unused `frontend/Dockerfile`, stub `docs/08-operations.md`, unused `nginx.conf`.

Canonical deploy docs: `docs/07-operations/deployment.md`

## Next recommended work

- [ ] Vercel frontend deploy + `NEXT_PUBLIC_API_BASE_URL`
- [ ] DNS CNAME: `api` → Render, `app` → Vercel
- [ ] Update Render CORS after frontend URL known
- [ ] CI: Docker image build, coverage, security scan (backlog)
- [ ] Wire Vitest/Playwright for frontend tests
