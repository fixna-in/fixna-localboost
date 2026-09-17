# Changelog

## Unreleased (post-cc95cc0 hotfixes, 2026-09-17)

- Fixed `PlatformJsonShapes` compilation failure (2026-09-17): added missing
  `in.fixna.platform.platform.dto.PlatformConnectionResponse` import; replaced
  non-existent `StatusOnly`/`ChannelList` nested `TypeReference`s with
  `List<PlatformConnectionResponse>` matching
  `GET /api/v1/platform-connections`. No API/migration change.
- Pinned Spring Boot entry point (2026-09-17): `backend/pom.xml`
  `spring-boot-maven-plugin` now sets
  `<mainClass>in.fixna.platform.FixnaApplication</mainClass>` (+
  `<finalName>fixna-api</finalName>`) to resolve `spring-boot:run`
  "Unable to find a single main class [FixnaApplication, Application]".
  `Application.java` duplicate retained (no deletion without approval);
  recommended follow-up is to delete it and keep `FixnaApplication` only.
  Run: `mvn -f backend/pom.xml spring-boot:run
  -Dspring-boot.run.profiles=local-nodocker`.

## Unreleased (WF03–WF14 implementation, committed as cc95cc0)

Backend/frontend implementation since 0.1.0, committed by the user as
`cc95cc0` on the `master` branch (2026-09-15). Verified locally:
`mvn -f backend/pom.xml clean test` → 178 tests, 0 failures, 3 skipped
(Testcontainers E2E auto-skips without local Docker), EXIT-0; frontend
`npm run build` → Next.js 15.5.25 compiled, EXIT-0.

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
- Observability (WF11, 2026-09-15, 158 backend tests green): structured-logging
  conventions doc, RequestLoggingFilter (fixna.access URI-only line), per-attempt
  platform.launch timers, idempotent OperationTimer, logging levels in
  application.yml. Closed by Workflow 14 logging rules (SLF4J-only sweep —
  no System.out/err in production code — fixna.error server-side logging,
  parameterized logging, MDC requestId on RequestIdFilter + cleared in finally,
  correlation fields incl. OTel-ready trace/span note).
- Creative drafts + E2E (WF12, verified 2026-09-15): creative module
  (DRAFT/READY lifecycle via canTransitionTo, tenant-scoped per-campaign
  CRUD, CreativeServiceTest ×8), ProdEnvironmentValidator prod data-plane
  fail-fast (+4 tests), FixnaEndToEndTest — Testcontainers full-journey
  suite that auto-skips without local Docker (runs in CI); WF12 checklist
  verified: clean install `mvn clean test` 178 tests/0 failures/3 skipped
  EXIT-0, journey + cross-tenant denial + invalid input + idempotent
  duplicate launch + demo mode all green, README Local development/Testing
  sections rewritten (profiles, compose, Swagger, E2E Docker note).
- Environment profiles (WF13, verified 2026-09-15): base application.yml
  rework (Hikari pool, Jackson ISO dates, fixna.app-env/ai.daily-quota/
  platform/billing keys) + AiSettings/PlatformSettings typed binding via
  AppSettingsConfig; per-profile files local / dev (HSTS on TLS) / test
  (test-only JWT secret, rate-limit 1000, mock AI, infra-free suite + E2E via
  @DynamicPropertySource) / staging (HSTS, no dev placeholder) / prod
  (fail-fast: empty JWT-secret default via SecurityStartupValidator;
  ProdEnvironmentValidator refuses localhost/default datasource);
  frontend/.env.example (NEXT_PUBLIC_API_BASE_URL/API only + APP_ENV,
  no-secrets note). Follow-up fixes committed with the WF12 README accuracy
  update: JwtSettings deleted, AiSettings/PlatformSettings wired,
  ProdEnvironmentValidator redis check on host/port with url fallback,
  AiProviderConfig factory honoring fixna.ai.provider; new tests
  AiProviderConfigTest ×3 + SettingsDefaultsTest ×2.
- Workflow 14 (logging/observability rules) accepted 2026-09-15:
  no System.out/err in production source, structured logging via SLF4J,
  MDC-based requestId correlation, safe server-side error logging without
  leaking driver/SQL internals at the envelope boundary.

## 0.1.0
- Initial Fixna LocalBoost AI-ready multi-tenant project blueprint and bootstrap skeleton.

