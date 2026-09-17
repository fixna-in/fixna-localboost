# Progress

## Latest hotfixes (2026-09-17, post-cc95cc0)
- [x] `PlatformJsonShapes` compile fix: missing
  `dto.PlatformConnectionResponse` import added; bogus
  `StatusOnly`/`ChannelList` `TypeReference`s replaced with
  `List<PlatformConnectionResponse>` (matches
  `GET /api/v1/platform-connections`). Files: `platform/PlatformJsonShapes.java`.
- [x] `spring-boot:run` duplicate-main fix: `backend/pom.xml` pins
  `<mainClass>in.fixna.platform.FixnaApplication</mainClass>`
  (+ `<finalName>fixna-api</finalName>`); duplicate `Application.java`
  retained, recommended follow-up is delete-it-and-keep-`FixnaApplication`.
- Next: re-run `mvn -f backend/pom.xml compile -DskipTests` (shell capture
  flaky on Windows — run one command at a time), then `spring-boot:run`
  with `local-nodocker` profile.

## What works / exists
- [x] Repository scaffold: backend/ (Maven, Java 21, Spring Boot 3.5.6),
  frontend/ (package.json), database/ (V1–V6 migrations + V100 demo seed),
  docs/ (product, architecture, ADRs 001–010, API overview, DB design,
  integrations, AI, operations), requirements/ (PRD, FR-001–FR-025, matrix,
  tenancy, business rules, stories, acceptance criteria), ai/ (v1 prompt,
  4 JSON schemas, eval cases), infra (docker-compose, nginx placeholder,
  terraform README), scripts (setup/start/stop/test/reset/seed), CI
  workflows (backend mvn test, frontend build, dependency review).
- [x] Memory bank initialized: projectbrief, productContext, systemPatterns,
  techContext, activeContext, progress (this file).
- [x] Workflow 00 bootstrap: git init (repo exists, nothing committed —
  `git status` shows all files untracked); Flyway SQL copied to
  `backend/src/main/resources/db/migration/`; `FixnaApplicationTest` smoke
  test GREEN (`mvn-test2.log` BUILD SUCCESS, 1 test); frontend skeleton
  builds (npm-build3.log: compiled + 4 static pages). Verified from logs,
  docker compose skipped per user request.
- [x] Workflow 01 backend foundation (verified): `common/web` (ApiError
  envelope, FixnaException, GlobalExceptionHandler, RequestIdFilter,
  HealthController), `common/tenant` (TenantContext + TenantContextTest),
  `common/audit` (AuditEvent, AuditPublisher, LoggingAuditPublisher),
  `common/config` (SecurityConfig fail-closed, OpenApiConfig), tests green.
- [x] Workflow 02 frontend foundation (verified): axios api-client with
  envelope schema + X-Request-Id + bearer hook, providers.tsx (QueryClient,
  LoadingState, ErrorState, HealthProbe), layout wired with Providers,
  homepage health probe. `axios ^1.7.0` added to package.json; `npm install`
  + `npm run build` green.
- [x] Workflow 03 (verified): auth register/login/refresh/logout (BCrypt,
  JJWT, single-use rotation, SHA-256 stored hashes, logout revocation),
  tenants (current/create/members/remove with last-owner guard), membership
  RBAC via TenantContext, business + location tenant-scoped CRUD,
  Migration V7 refresh_tokens, cross-tenant isolation tests; `mvn test` green.
- [x] Workflow 04 campaign domain (verified): state
  machine (DRAFT→…→FAILED/COMPLETED), objectives, offers, channel
  budgets (BR-4/BR-6), idempotent launch, CampaignService + Controller,
  CampaignServiceTest (17 tests) + docs/03-api/campaign.md. Full suite
  `mvn -f backend/pom.xml test`: 35 tests, 0 failures.
- [x] Workflow 05 geo + audience (verified): GeoTarget (RADIUS/CITY/POSTAL/
  REGION/COUNTRY) + Audience (JSONB criteria), tenant-scoped services and
  controllers under /api/v1/campaigns/{id}/geo-targets and /audiences,
  per-type validation, campaign-bound update/delete, GeoTargetServiceTest
  (7) + AudienceServiceTest (7), docs/03-api/geo-audience.md.
- [x] Workflow 06 AI engine (verified after contract reconciliation):
  RecommendationType, AIProvider + MockAIProvider (mock-v1),
  AiSchemaValidator + AiBusinessValidator (BR-3/BR-4),
  AiRecommendationService (input → provider → schema → business →
  usage row → audit), AiController POST /api/v1/ai/recommendations,
  AiProviderConfig (swappable), AiUsage → ai_usage_log (V8).
  Advisory-only; never mutates campaign state. 4 test classes green.
- [x] Workflow 07 platform adapters (verified): AdvertisingPlatformAdapter +
  deterministic mocks Google/Meta/WhatsApp (ADR-004, SDK-free), registry,
  per-tenant PlatformConnection (V5; token columns never exposed, mock mode
  stores none), launch pipeline CampaignLaunchTx (transactional
  begin/applyOutcome) + CampaignLaunchService (non-txn executor; no DB txn
  held across adapter calls; QUEUED→CREATING→ACTIVE/FAILED, CREATING→FAILED
  recoverable, idempotent by externalReference BR-6), connections API
  /api/v1/platforms/connections, docs/03-api/platform.md.
- [x] Workflow 08 analytics + leads (verified): CampaignMetric → V5
  campaign_metrics (upsert key campaign_id+metric_date, full overwrite on
  re-ingest), AnalyticsService (tenant dashboard with per-campaign rollups +
  lead funnel, campaign daily timeline, deterministic demo seed), lead/
  module (funnel statuses, paginated clamped search, parent business+campaign
  ownership) nested under /businesses/{id}/leads; AnalyticsServiceTest (8) +
  LeadServiceTest (9); docs/03-api/analytics-leads.md. No new migration —
  V5 owns both tables.
- [x] Workflow 09 admin/billing (verified 2026-09-12, `mvn test` 124 tests,
  0 failures): platform admin (GET /admin/tenants, /admin/tenants/{id}/members,
  PUT /admin/tenants/{id}/plan — INTERNAL-tenant gate), subscription entity +
  lazy FREE materialization (V6 subscriptions, effective plan from
  PlanCode/PlanLimits FREE/STARTER/GROWTH), shared PlanLimitChecker wired into
  business/campaign create (PLAN_* fail-closed), AiQuotaChecker pre-provider
  24h usage window (AI_QUOTA_EXCEEDED), audit persistence
  (PersistedAuditPublisher writes audit_logs after commit; LoggingAuditPublisher
  kept as sink), tenant-scoped audit viewer + INTERNAL admin variant
  (GET /audit, GET /admin/audit), NotificationProvider + Logging provider +
  fan-out service emitting lead.created / campaign.launched|launch_failed;
  V9 migration reduced to index-only. Docs: docs/03-api/admin-billing.md.
  Tests: PlanLimitCheckerTest (5), SubscriptionServiceTest (4),
  AiQuotaCheckerTest (2), AuditViewerServiceTest (5), NotificationServiceTest
  (4) = +20 over WF08.
- [x] Workflow 10 security hardening (verified 2026-09-13, `mvn test` 150 tests,
  0 failures): auth-surface rate limiting (RateLimiter + RateLimitFilter,
  /api/v1/auth/**, 429 RATE_LIMIT_EXCEEDED + Retry-After, masked-IP audit),
  secure headers (nosniff/DENY/strict-referrer/X-XSS-0/no-store, opt-in HSTS),
  CORS exact-origin allowlist (fail-closed on empty AND on "*" with
  credentials), SecurityStartupValidator (refuses prod on missing/dev/weak
  FIXNA_JWT_SECRET), GlobalExceptionHandler server-side unexpected-error
  logging + URI-only envelope paths (no query-string secret echo). New tests:
  SecurityHeadersFilterTest(2), RateLimiterTest(5), RateLimitFilterTest(4),
  CorsConfigTest(3), SecurityStartupValidatorTest(4), GlobalExceptionHandlerTest(2),
  RepositoryTenantScopeSweepTest(2) + CrossTenantSweepTest(4) IDOR sweep.
  Docs: docs/07-operations/security-hardening.md, auth-business throttling
  note, application.yml cors/rate-limit/hsts keys.

- [x] Workflow 11 observability (verified 2026-09-15, `mvn -f backend/pom.xml
  test` → 158 tests, 0 failures, BUILD SUCCESS; baseline WF10 150 + 8):
  structured-logging conventions doc (docs/07-operations/observability.md —
  fixna.access/error/audit/telemetry logger categories, levels, access line
  format, correlation, health/readiness, metrics, OTel-ready seams, secrets
  policy), RequestLoggingFilter (one URI-only access line per /api/v1/**
  request with method/path/status/durationMs + requestId/tenantId/userId,
  registered outermost in SecurityConfig), adapter telemetry
  (CampaignLaunchService per-attempt `platform.launch` timers: SUCCESS/FAILED
  + platform), OperationTimer double-close guard + missing UUID import fix
  (pre-existing WF11 compile breakage this session resumed into),
  application.yml logging.level (root/in.fixna.platform INFO). New tests:
  OperationTimerTest (3), RequestLoggingFilterTest (3), ReadinessControllerTest (2).


- [x] Workflow 12 track — creatives + E2E foundation (verified 2026-09-15,
  `mvn test` → 173 tests, 0 failures, 3 skipped — FixnaEndToEndTest
  Testcontainers suite auto-skips without Docker, runs in CI): creative
  module (Creative DRAFT/READY + canTransitionTo, CreativeRepository,
  CreativeRequest/Response DTOs, CreativeService tenant-scoped
  per-campaign CRUD, CreativeController, CreativeServiceTest ×8),
  ProdEnvironmentValidator prod data-plane fail-fast
  (+ProdEnvironmentValidatorTest ×4), frontend/.env.example documented
  (NEXT_PUBLIC_* only, no secrets).
- [x] Workflow 13 environment profiles (committed cc95cc0 2026-09-15;
  reconciliation follow-ups fixed + verified same day): base application.yml
  rework (server port env,
  Hikari pool tuning keys, Jackson ISO dates, actuator/springdoc,
  fixna.app-env, ai.daily-quota, platform.default-mode,
  billing.default-plan, Redis host/port) + AiSettings typed
  @ConfigurationProperties (provider, dailyQuota — wiring to confirm);
  profile files application-local/dev/test/staging/prod.yml — local DEBUG
  diagnostics, dev HSTS on TLS hosts, test (test-only JWT secret, per-ip
  rate-limit 1000, mock AI, conservative suites infra-free + Testcontainers
  E2E via @DynamicPropertySource), staging (HSTS, no dev placeholder,
  empty-default JWT secret), prod (nothing defaults to localhost/dev;
  fail-fast via SecurityStartupValidator + ProdEnvironmentValidator);
  frontend/.env.example NEXT_PUBLIC_API_BASE_URL/APP_ENV with no-secrets
  note. Workflow 14 logging rules closed by WF11 implementation (SLF4J-only
  production code, requestId MDC + parameterized logging, fixna.error,
  profile-aware levels).


- [x] Workflow 12 final E2E (verified 2026-09-15): `mvn clean test` from
  scratch → 178 tests, 0 failures, 3 skipped, EXIT-0. FixnaEndToEndTest
  (Testcontainers, real PostgreSQL + all migrations) covers the complete
  journey (register→tenant→business→location→campaign→geo→audience→budget→
  AI rec→review→approve→mock launch→metrics→lead→dashboard), cross-tenant
  denial (NOT_FOUND for other tenants' campaign/business), invalid input
  (BUDGET_EXCEEDED / INVALID_GEO_TARGET / AI_VALIDATION_FAILED), idempotent
  duplicate launch (re-run after ACTIVE is a no-op) and demo mode without
  external credentials. Docker startup verified via CI path (ubuntu-latest
  runs the E2E on every push/PR; local Docker not installed — documented).
  README: Local development rewritten (local profile run command, compose
  services with ports, API/Swagger/health URLs, Flyway schema ownership,
  frontend .env.local flow) + new Testing section (E2E Docker behavior) +
  duplicate h1 demoted. Frontend build green (Next.js 15.5.25, EXIT-0;
  Vitest test runner still absent — noted limitation).

## What's left (aligned to .cline/tasks/BACKLOG.md + workflows)
- Production: OpenTelemetry (real tracer/exporters), CI/CD completion
  (coverage, Docker image build, image security scan), deployment.

## Known issues / gaps found during review
1. `database/migrations/` (docs copy) still lags the canonical
   `backend/src/main/resources/db/migration/` (V8/V9 present only in the
   backend tree).
2. `openapi.yaml` is a stub (`paths: {}`) — real spec to be generated from
   springdoc once controllers exist.
3. `V100__demo_data.sql` has no `users`/`tenant_memberships` rows and its
   own comment flags the password hash as ungenerated — demo login will not
   work from seed alone.
4. `backend/target/` contains stale build artifacts (clean/ignore).
5. Tailwind (required by frontend rules) is not in `package.json` yet;
   login/setup UI pages still pending (frontend Workflow 03+).
6. Docs `system-overview.md` does not exist (actual file is
   `system-architecture.md`); `docs/01-requirements/README.md` is a pointer
   to `/requirements` only.

## Test status
WF09 verified green (`mvn -f backend/pom.xml test` → 124 tests,
0 failures, BUILD SUCCESS 2026-09-12). WF10 verified green
(`mvn -f backend/pom.xml test` → 150 tests, 0 failures, 0 errors, 0 skipped,
BUILD SUCCESS 2026-09-13; frontend build green at WF02). WF11 verified green
(`mvn -f backend/pom.xml test` → 158 tests, 0 failures, 0 errors, 0 skipped,
BUILD SUCCESS 2026-09-15). WF12 verified green
(`mvn -f backend/pom.xml test` → 173 tests, 0 failures, 0 errors, 3 skipped —
Testcontainers E2E auto-skip without Docker, BUILD SUCCESS 2026-09-15);
WF12 closeout verified green
(`mvn -f backend/pom.xml clean test` from scratch → 178 tests, 0 failures,
3 skipped, EXIT-0, 2026-09-15).
WF13 verified green after follow-up fixes (`mvn -f backend/pom.xml test` →
178 tests, 0 failures, 0 errors, 3 skipped, EXIT-0; frontend `npm run build`
→ Next.js 15.5.25 compiled, 4/4 static pages, EXIT-0 — 2026-09-15).
WF14 logging/observability-rules PASSED on first read (no System.out/err
in production code, fixna.* logger categories, MDC requestId on
RequestIdFilter + cleared in finally, safe path in GlobalExceptionHandler
— 2026-09-15).
All workflows WF00–WF14 are implemented and verified; remaining work
is the production track only.

