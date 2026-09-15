# Changelog

## Unreleased (WF03–WF11 implementation, uncommitted)

Backend/frontend implementation since 0.1.0 (all verified green, still
uncommitted — see git status):

- Auth + tenant + business (WF03): BCrypt/JJWT register/login/refresh/logout
  with single-use refresh rotation, tenant create/current/members/remove with
  last-owner guard, tenant-scoped business + location CRUD, V7 refresh_tokens.
- Campaign domain (WF04): DRAFT→…→FAILED/COMPLETED state machine, objectives,
  offers, channel budgets (BR-4/BR-6), idempotent launch.
- Geo + audience (WF05): RADIUS/CITY/POSTAL/REGION/COUNTRY targets, validated
  audience JSON criteria, tenant-scoped nested APIs.
- AI engine (WF06): AIProvider + MockAIProvider (mock-v1), schema + business
  validation, advisory-only recommendation flow with usage/cost log (V8).
- Platform adapters (WF07): SDK-free deterministic Google/Meta/WhatsApp mocks,
  registry, per-tenant connections (V5), transactional launch pipeline
  (begin/applyOutcome) with bounded retries.
- Analytics + leads (WF08): dashboard/campaign timeline, idempotent metric
  upsert, deterministic demo seed; lead capture + funnel + paginated search.
- Admin/billing/notifications/audit (WF09): platform admin, plan limits +
  AI quota (V9), notification service, persisted audit viewer.
- Security hardening (WF10): auth-surface rate limiting (429 + Retry-After),
  secure headers, exact-origin CORS fail-closed, prod JWT-secret startup
  guard, server-side unexpected-error logging, cross-tenant IDOR sweep tests.
- Observability (WF11, 158 tests green 2026-09-15): structured-logging
  conventions doc, RequestLoggingFilter (fixna.access URI-only line),
  per-attempt platform.launch timers, idempotent OperationTimer, logging
  levels in application.yml.
- Environment profiles (WF13, in progress): base application.yml rework
  (Hikari pool, Jackson dates, fixna.app-env/ai.daily-quota/platform/billing
  keys) + AiSettings (@ConfigurationProperties) typed binding.

## 0.1.0
- Initial Fixna LocalBoost AI-ready multi-tenant project blueprint and bootstrap skeleton.

