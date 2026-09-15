# Fixna Backlog

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
- [ ] Budget
- [ ] Creatives
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
- [ ] OpenTelemetry (real tracer/exporters)
- [ ] CI/CD
- [ ] E2E
- [ ] Deployment
