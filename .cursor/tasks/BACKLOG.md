# Fixna Backlog

**Current release:** 1.0.1 (2026-09-28) — shared demo live at fixna.in (GitHub org fixna-in)

## Foundation
- [x] Bootstrap repository
- [x] Backend foundation
- [x] Frontend foundation

## SaaS
- [x] Authentication
- [x] Tenant management
- [x] Membership/RBAC
- [x] Business management


## LocalBoost
- [x] Campaigns (CRUD + lifecycle + offers + budget/channels + idempotent launch)
- [x] Geo targeting (radius/city/postal/region/country + tenant-scoped APIs)
- [x] Audience (validated JSON criteria + tenant-scoped APIs)
- [x] Budget (campaign channel allocations BR-4/BR-6 + replace-all endpoint)
- [x] Creatives (DRAFT/READY lifecycle, tenant-scoped per-campaign CRUD)
- [x] AI recommendations (provider abstraction + mock, schema+business validation, usage/cost log)
- [x] Mock advertising adapters (Google/Meta/WhatsApp mocks, tenant connections, idempotent launch pipeline)
- [x] Analytics (dashboard/campaign timeline/metric ingest upsert + deterministic demo seed)
- [x] Leads (CRUD + status funnel + paginated clamped search + parent business/campaign ownership)

## Platform
- [x] Admin
- [x] Billing foundation
- [x] Notifications
- [x] Audit

## Production
- [x] Security hardening (WF10: auth-surface rate limiting, secure headers,
  CORS allowlist, prod JWT-secret startup guard, server-side unexpected-error
  logging, envelope path without query strings, cross-tenant sweep tests)
- [x] Observability foundation (WF11: structured logging conventions,
  request/access logging, operation timings, AI + platform adapter telemetry,
  health/readiness probes, OTel-ready seams)
- [x] Logging & observability rules (WF14: SLF4J-only, correlation/MDC,
  safe error logging — closed by WF11 implementation)
- [x] Environment profiles (WF13: base + local/dev/test/staging/prod files,
  fail-fast prod validators, typed AiSettings, frontend .env.example —
  verification pending)
- [x] Environment profiles (WF13: base + local/dev/test/staging/prod files,
  fail-fast prod validators, typed AiSettings/PlatformSettings wired,
  AiProvider factory honoring fixna.ai.provider, frontend .env.example —
  verified 178 backend tests + frontend build green 2026-09-15)
- [x] OpenTelemetry foundation (Micrometer OTel bridge, OTLP export in
  dev/staging/prod, trace/span MDC via RequestIdFilter, export disabled in
  local/test)
- [x] Shared demo deployment docs + Render Blueprint (`render.yaml`, Dockerfile,
  staging profile, Neon SQL seeds, `docs/07-operations/deployment.md`)
- [x] Render API live (`fixna-localboost.onrender.com`, health UP)
- [ ] CI/CD (Docker image build in CI, coverage gates, security scan)
- [x] E2E (WF12 verified 2026-09-15: Testcontainers full-journey suite —
  register→…→launch→metrics→leads→analytics, cross-tenant denial, invalid
  input, idempotent launch, demo mode; runs on CI ubuntu runners with
  Docker; local clean-install `mvn clean test` 178 green EXIT-0; README
  accuracy fixed)
- [x] Environment profiles (WF13 verified 2026-09-15: 178 backend tests +
  frontend build green; committed as cc95cc0)
- [x] Deployment — API on Render; Vercel + custom DNS live (`app.fixna.in`, `api.fixna.in`)
- [x] Brand mark unified (favicon, header, auth hero — `frontend/src/brand/`)

## Known gaps
- `docs/03-api/openapi.yaml` is still a stub — generate from springdoc when ready.
- Vitest/Playwright frontend test runner not yet wired (build passes; unit tests
  exist for display/api-schemas only).
