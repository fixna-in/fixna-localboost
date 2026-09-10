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
- [x] Workflow 01 backend foundation (code complete, test run pending):
  `common/web` (ApiError envelope, FixnaException, GlobalExceptionHandler,
  RequestIdFilter, HealthController), `common/tenant` (TenantContext +
  TenantContextTest), `common/audit` (AuditEvent, AuditPublisher,
  LoggingAuditPublisher), `common/config` (SecurityConfig fail-closed,
  OpenApiConfig), `common/web` test (RequestIdAndErrorTest).
- [x] Workflow 02 frontend foundation (code complete, build pending):
  axios api-client with envelope schema + X-Request-Id + bearer hook,
  providers.tsx (QueryClient, LoadingState, ErrorState, HealthProbe),
  layout wired with Providers, homepage health probe. `axios ^1.7.0`
  added to package.json (node_modules install still pending).
- [ ] Everything else is NOT started (see below).

## What's left (aligned to .cline/tasks/BACKLOG.md + workflows)
- Verify: `mvn -f backend/pom.xml test` green for the 3 new tests;
  `npm install --prefix frontend` (axios) + `npm run build --prefix frontend`
  green; then initial git commit; then workflow 03.
- SaaS: auth (register/login/refresh/logout), tenants, membership/RBAC,
  business + location CRUD, tenant-isolation tests.
- LocalBoost: campaign CRUD + lifecycle state machine, offers, objectives,
  budgets/channels, geo + audience models, AI engine (AIProvider,
  MockAIProvider, schema+business validation, usage tracking, endpoints),
  creatives, mock adapters (Google/Meta/WhatsApp), idempotent launch,
  metrics, leads.
- Platform: admin (tenants/audit), billing foundation, notifications, audit.
- Production: security hardening, OpenTelemetry, CI/CD completion, E2E
  (register→…→mock launch→metrics), deployment.

## Known issues / gaps found during review
1. `backend/src/main/resources/db/migration/` holds only `.gitkeep`; real
   SQL lives in `database/migrations/`. With `ddl-auto: validate` the app
   cannot create or validate schema on a fresh DB until locations are
   reconciled.
2. `openapi.yaml` is a stub (`paths: {}`) — real spec to be generated from
   springdoc once controllers exist.
3. `V100__demo_data.sql` has no `users`/`tenant_memberships` rows and its
   own comment flags the password hash as ungenerated — demo login will not
   work from seed alone.
4. No git repository (`fatal: not a git repository`).
5. `backend/target/` contains stale build artifacts from scaffold generation
   (should be cleaned/ignored).
6. Frontend has zero source files; Tailwind (required by frontend rules) is
   not in `package.json` yet.
7. Docs `system-overview.md` does not exist (actual file is
   `system-architecture.md`); `docs/01-requirements/README.md` is a pointer
   to `/requirements` only.

## Test status
No tests exist and none have been run. CI expects `mvn -f backend/pom.xml
test` and `npm install/build --prefix frontend` to pass post-bootstrap.

