# Progress

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

## What's left (aligned to .cline/tasks/BACKLOG.md + workflows)
- Workflow 12: final E2E.
- LocalBoost remaining: budget, creatives.
- Production: OpenTelemetry (real tracer/exporters), CI/CD completion, E2E
  (register→…→mock launch→metrics), deployment.

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
BUILD SUCCESS 2026-09-15).

