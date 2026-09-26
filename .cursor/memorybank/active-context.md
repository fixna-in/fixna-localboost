# Active Context — Fixna LocalBoost

**Last updated:** 2026-09-26  
**Release:** **1.0.0** — first major release

## Current focus

**v1.0.0 shipped.** Shared demo on **fixna.in** is fully live (API, frontend, DNS).
Post-release: production smoke tests and CI hardening for 1.1.0.

| App | URL |
|-----|-----|
| Dashboard | https://app.fixna.in/dashboard |
| Campaigns | https://app.fixna.in/campaigns |
| API health | https://api.fixna.in/actuator/health |

## Live stack

| Layer | Provider | Notes |
|-------|----------|-------|
| Database | Neon PostgreSQL (`fixna`) | Manual SQL seed only |
| API | Render (`fixna-localboost`) | `staging` profile in Dockerfile |
| Frontend | Vercel (`frontend/`) | Next.js 15 |

## Brand mark (unified)

Canonical **`f•`** on `#163e32` with lime dot `#c7ed94`:

| Location | File / component |
|----------|------------------|
| Favicon | `frontend/src/app/icon.svg` |
| Apple touch | `frontend/src/app/apple-icon.svg` |
| Header / sidebar | `BrandMarkIcon` in `components/ui.tsx` |
| Login/register hero | `BrandMarkIcon` in `components/auth-layout.tsx` |
| Source of truth | `frontend/src/brand/brand-mark-graphic.tsx` |

## Backend note — RequestIdFilter

- Injects `Optional<Tracer>` (Spring supplies empty when no tracer bean).
- Explicit `micrometer-tracing` in `pom.xml` (IDE classpath).
- Copies `traceId`/`spanId` into MDC when span active.

## Demo data (Neon — manual)

1. `tools/sql/neon-demo-seed.sql` — `owner@example.com`, tenant, business
2. `tools/sql/neon-demo-data.sql` — campaigns, leads, metrics

Password: `tools\password-tool.cmd hash` → SQL `UPDATE users SET password_hash = ...`

## Staging profile

- No auto seed, Redis off, no Spring default password
- Mock AI + mock platform adapters

## Key env vars

**Render:** `SPRING_PROFILES_ACTIVE`, `SPRING_DATASOURCE_URL`, `POSTGRES_*`,
`FIXNA_JWT_SECRET`, `FIXNA_CORS_ALLOWED_ORIGINS` (must include `https://app.fixna.in`)

**Vercel:** `NEXT_PUBLIC_API_BASE_URL=https://api.fixna.in/api`, `NEXT_PUBLIC_APP_ENV=demo`

## Agent handoff

Full no-secrets brief: `.cursor/memorybank/chatgpt-handoff.md`

## Next recommended work

- [ ] E2E smoke on production URLs
- [ ] CI: Docker build, coverage, security scan
- [ ] Vitest/Playwright frontend tests
- [ ] OpenAPI generation from springdoc
