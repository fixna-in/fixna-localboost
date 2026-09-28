# Changelog

## Unreleased

_No changes yet._

## 1.0.1 — 2026-09-28

**Post-release maintenance** — shared demo fully operational on [fixna.in](https://fixna.in)
under GitHub org [fixna-in](https://github.com/fixna-in).

### Health & observability

- Aggregated `GET /api/v1/health` — overall status, per-component probes (`db`,
  `flyway`, `platform`, …), `version`, `deployedAt`, `environment`, `service`.
- `PlatformHealthService` + `PlatformHealthIndicator`; Maven `build-info` for version.
- Optional `FIXNA_DEPLOYED_AT` on Render; actuator health shows component details.
- Removed custom `FlywayHealthIndicator` (bean name `flyway` conflicted with Spring
  Boot Flyway auto-config — use built-in actuator `flyway` probe).

### Frontend & CI

- Next.js **16.3.6** (from 15.5.x); `themeColor` moved to `viewport` export.
- `dependency-review` job merged into `.github/workflows/ci.yml` (PR-only);
  removed broken `security-scan.yml` `on: push` trigger.

### Local development

- `start-local-demo.ps1` runs `mvn clean compile` before `spring-boot:run` (avoids
  stale bytecode / `Lookup method resolution failed` on `CampaignLaunchService`).
- Flyway **11.20.0** for PostgreSQL 18 local dev; `CampaignLaunchAttempt` extracted.
- `ApplicationContextLoadTest` — full Spring context smoke (Testcontainers).

### Deployment & org

- GitHub repository under **fixna-in**; Render API and Vercel frontend reconnected.
- Live: https://app.fixna.in · https://api.fixna.in/api/v1/health

## 1.0.0 — 2026-09-26

**First major release** of Fixna LocalBoost: production-oriented multi-tenant MVP
with shared demo live at [app.fixna.in](https://app.fixna.in) and API at
[api.fixna.in](https://api.fixna.in).

### Platform (MVP)

- Multi-tenant SaaS backend (Java 21, Spring Boot 3.5, PostgreSQL, Flyway V1–V9):
  auth/JWT, tenants/RBAC, businesses, campaigns (lifecycle + idempotent launch),
  geo/audience/creatives, AI recommendations (mock), mock ad platform adapters,
  analytics, leads, admin, billing, audit.
- Next.js 15 frontend: login/register, dashboard, businesses, campaigns, leads.
- Security: tenant isolation, rate limiting, CORS allowlist, secure headers, prod
  JWT fail-fast, no generated Spring security password on staging.
- Observability: Log4j2 structured logging, MDC correlation, Micrometer OTel bridge,
  `RequestIdFilter` with `X-Request-Id` and optional trace/span MDC (`Optional<Tracer>`).

### Shared demo deployment

- **Stack:** Neon PostgreSQL + Render API (Docker) + Vercel frontend.
- `render.yaml` Blueprint; `backend/Dockerfile` with `SPRING_PROFILES_ACTIVE=staging`.
- Staging profile: Redis disabled, no startup seed, mock AI/platforms.
- Neon manual SQL: `tools/sql/neon-demo-seed.sql`, `tools/sql/neon-demo-data.sql`.
- Docs: `docs/07-operations/deployment.md`, `.cursor/memorybank/`.

### Brand identity

- Unified Fixna mark (`f•` on `#163e32`, lime `#c7ed94`): favicon, header,
  login/register hero. Source: `frontend/src/brand/brand-mark-graphic.tsx`.

### Removed

- Abandoned Fly.io config and deploy workflow; unused `frontend/Dockerfile`,
  `docs/08-operations.md`, `infrastructure/nginx/nginx.conf`.

### Version bump

- Backend `pom.xml` and frontend `package.json`: **1.0.0**.

## Unreleased — OpenTelemetry and CI hardening

- Added Micrometer OpenTelemetry bridge and OTLP trace exporter (export
  disabled in `local`/`test`; enabled in `dev`/`staging`/`prod` via
  `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`).
- `RequestIdFilter` copies active span `traceId`/`spanId` into MDC for
  log/trace correlation.
- Removed duplicate `Application.java` main class (`FixnaApplication` only).
- Fixed Log4j2 test configuration and `PlatformException.getPlatform()` compile
  error in campaign launch logging.
- CI frontend job now uses `npm ci` with `package-lock.json` cache.

## Unreleased — Cursor agent configuration

- Migrated AI agent configuration from Cline (`.cline/`, `.clinerules/`) to
  Cursor (`.cursor/rules/`, `.cursor/workflows/`, `.cursor/tasks/`).
- Converted 17 Cline rule files into 12 focused `.mdc` rule files with YAML
  frontmatter (`alwaysApply` and file-glob scoping).
- Updated `AGENTS.md`, `README.md`, branch strategy docs, and module READMEs
  to reference Cursor paths.
- Removed stale scratch files (root `*.log`, `progress.md`, frontend build
  verification artifacts) and obsolete directories (`database/` duplicate
  migrations, Cline `memorybank/`).
- Extended `.gitignore` for TypeScript build info and agent scratch files.

## Unreleased — Log4j2 structured logging

- Replaced the default Spring Boot logging backend with Log4j2:
  `spring-boot-starter-log4j2` added, `spring-boot-starter-logging` excluded,
  so SLF4J -> Log4j2 is the single logging implementation.
- Added `log4j2-spring.xml` (human-readable pattern for `local`/`test`/default,
  structured JSON with `service`/`environment` plus all MDC fields for
  `dev`/`staging`/`prod`) and a quiet `log4j2-test.xml` for suites.
- Added MDC correlation (`traceId`, `spanId`, `requestId`, `tenantId`,
  `userId`, `campaignId`, `operation`) via `LoggingContext`, populated from the
  JWT-derived tenant context — never from client input — and cleared per request.
- Added `SensitiveDataMasker` and safe `X-Request-Id` sanitization so secrets,
  tokens and raw query strings never reach logs.
- Added business/technical log lines for auth, campaign lifecycle, launch
  orchestration, AI recommendations and leads. No schema or API contract change.

## Unreleased — password utility launcher fix

- Pass the full Maven dependency classpath through a Java argument file instead
  of CMD `set /p`, which truncates long lines and prevented BCrypt from loading.
- Added a real-launcher regression check for dependency loading from a different
  working directory, without supplying or recording a password.


## Unreleased — consolidated local PostgreSQL profile

- Kept only `application-local.yml` for local development: persistent
  `localhost:5432/localboost`, user `postgres`, runtime `POSTGRES_PASSWORD`.
- Removed superseded local-pg/local-nodocker profiles and embedded PostgreSQL
  configuration/dependency. Launcher now selects `local`; automatic fixture
  loading remains disabled. Separate SQL setup and existing data are unchanged.
- Updated seeder profile guards/tests and added local configuration regression
  coverage. Non-local environment profiles remain unchanged.


## Unreleased — local startup test fixture

- Simplified local setup: standalone `tools/sql/schema.sql` (V1–V9 plus baseline),
  standalone `tools/sql/demo-data.sql`, and one service launcher via `.cmd`/PowerShell.
  Persistent startup no longer creates databases, seeds fixtures or prints passwords.
  Removed superseded create/load wrappers and obsolete fixture-launcher tests.
  Disposable PostgreSQL SQL tests passed: full fixture, repeat-run preservation,
  baseline record and rejection of nonempty schemas. No applied migration changed.


- Added persistent `local-pg` startup against localhost:5432/localboost as the
  demo launcher's default. Database and demo passwords are supplied at runtime;
  existing records are preserved. Added create-if-missing database and psql seed
  wrapper scripts, local-profile guard tests, and setup documentation. Embedded
  PostgreSQL remains available with `-EmbeddedDatabase`. No applied migrations
  changed. Validation of this change is pending.


- Fixed demo launcher process-exit handling on Windows PowerShell: retain the
  child process handle and distinguish an unavailable exit code from a real
  npm failure. No dependency changes or install-check bypass. Process exit-code
  regressions (0 and 7) and nine fixture regressions passed; full frontend
  launcher smoke-test completion remains unconfirmed in the agent terminal.


- Fixed logout returning HTTP 500 during demo startup: JWT authentication now
  runs for logout, and only register/login/refresh remain public auth routes.
  Added a missing-principal guard, OpenAPI endpoint documentation, and MVC
  security-chain regression tests for authenticated, anonymous, invalid-token,
  revoked-membership, and public-auth requests.


- Fixed Windows PowerShell REST-array counting in the demo launcher, which
  incorrectly rejected the two-campaign fixture after successful login. Kept
  strict fixture checks and added count-only failure diagnostics plus a
  dependency-free regression script: tools/test-local-demo-fixture.ps1.

- Added Windows `tools/start-local-demo.cmd` plus PowerShell orchestration:
  bounded startup, generated local password, API login/data verification,
  optional frontend startup, process cleanup and smoke-test mode.
- Expanded the atomic local fixture with two demo campaigns, offers, channels,
  geo/audience/creative data, ten leads, seven metric days and STARTER subscription.
  No external execution or platform-admin privilege. Extended integration assertions.

- Added a separate SQL fixture and local-nodocker startup loader for a test
  owner, workspace, business and location. Requires FIXNA_TEST_USER_PASSWORD;
  stores only BCrypt hashes. Existing emails skip the entire fixture unchanged.
- Restricted to local-nodocker/local; opt out with FIXNA_TEST_DATA_ENABLED=false.
  Added unit/environment checks and PostgreSQL integration tests for repeat runs,
  password compatibility and existing-account preservation. No schema/API changes.


## Unreleased (frontend API integration, 2026-09-17)

- Redesigned the frontend with a green/lime visual identity, split-panel
  authentication pages, responsive workspace navigation, dashboard metric cards,
  campaign tables, business cards, lead badges, and guided empty states.
  Existing API calls remain in place; demo metrics are explicitly labeled.
  Added metric-formatting tests. Build completion and browser appearance are
  not yet verified; do not treat this visual pass as production sign-off.


- Added the missing global stylesheet and root-layout import. Shared responsive
  navigation, forms, error states, and login/register cards now have baseline
  styling rather than browser defaults. Uses native CSS; no new dependencies
  or API changes. Tailwind remains unconfigured.

- Frontend wired to the built backend (`/api/v1`, JWT from refresh rotation):
  fixed `api-client` base URL to honor canonical `NEXT_PUBLIC_API_BASE_URL`
  (`.../api` + `/v1` auto-append, legacy `NEXT_PUBLIC_API_URL` fallback),
  added `AuthProvider` + refresh restore, and built journey pages —
  login/register, dashboard (`GET /analytics/dashboard` + demo-seed),
  businesses + detail/locations, campaigns list/new/detail (transitions,
  idempotent launch + execute-launch, 50-50 channel split, advisory AI
  strategy call, geo/audience/creative sub-resources), leads
  (paginated + status advance). Added `@hookform/resolvers` dep and
  `api-schemas.test.ts` (auth/campaign/error mapping). No backend change.
  Verify locally: `npm --prefix frontend install` then `npm run build`
  from `frontend/` (blocked in-agent by PowerShell execution policy).

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

