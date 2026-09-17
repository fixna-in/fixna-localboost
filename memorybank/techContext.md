# Tech Context — Fixna LocalBoost

## Stack
- Backend: Java 21, Spring Boot 3.5.6 (parent), spring-boot-starter-web,
  security, validation, data-jpa, actuator, data-redis; Lombok (optional);
  PostgreSQL driver (runtime); Flyway core + Flyway PostgreSQL dialect;
  springdoc-openapi 2.8.13; test: spring-boot-starter-test, Testcontainers
  junit-jupiter + postgresql. GroupId `in.fixna`, artifact `fixna-api`.
- Frontend (implemented): Next.js ^15.5 app router, React ^19.1, TS ^5,
  axios api-client (ApiError envelope, X-Request-Id, bearer hook),
  TanStack Query providers (QueryClient, LoadingState, ErrorState,
  HealthProbe); Tailwind/React Hook Form/Zod/Vitest not yet in
  package.json.
- Data: PostgreSQL 17 (docker), Redis 8-alpine, Mailhog (dev mail).
  Backend connects via `SPRING_DATASOURCE_URL` (default
  `jdbc:postgresql://localhost:5432/fixna`); Redis via
  `spring.data.redis.host/port` (`REDIS_HOST`/`REDIS_PORT`); Hikari pool
  via `DB_POOL_MAX`/`DB_POOL_MIN`/`DB_CONN_TIMEOUT_MS`.
- Config (WF13): typed settings records — JwtProperties + CorsProperties
  wired via SecurityConfig @EnableConfigurationProperties; JwtSettings,
  AiSettings, PlatformSettings present but NOT registered (follow-up);
  RateLimitFilter/SecurityHeadersFilter bind via @Value keys. Env vars:
  `FIXNA_JWT_SECRET`, `FIXNA_JWT_ACCESS_TTL` (PT15M),
  `FIXNA_JWT_REFRESH_TTL` (P7D), `FIXNA_CORS_ALLOWED_ORIGINS`,
  `FIXNA_RATE_LIMIT_PER_IP_PER_MINUTE`, `FIXNA_HSTS_ENABLED`,
  `FIXNA_AI_PROVIDER`, `FIXNA_AI_DAILY_QUOTA`, `FIXNA_PLATFORM_MODE`,
  `FIXNA_BILLING_DEFAULT_PLAN`, `FIXNA_APP_ENV`. Profiles:
  application-{local,dev,test,staging,prod}.yml; frontend `.env.example`
  exposes only `NEXT_PUBLIC_API_BASE_URL`/`NEXT_PUBLIC_APP_ENV`.
- `application.yml`: JPA `open-in-view: false`, `ddl-auto: validate` (Flyway
  owns schema; locations `classpath:db/migration`); server port from
  `SERVER_PORT` (8080); actuator exposes health/info/metrics/prometheus;
  springdoc api-docs `/v3/api-docs`, swagger `/swagger-ui.html`.

## API contract (planned — docs/03-api/api-overview.md; openapi.yaml is `{}`)
Base `/api/v1`. Auth: POST register/login/refresh/logout. Tenant:
GET /tenants/current, POST /tenants, GET members. Business: CRUD
/businesses (+/{id}). Campaign: CRUD /campaigns, POST /{id}/recommendation,
/approve, /launch, /pause, /resume; GET /{id}/metrics. Leads: GET/POST
/leads, PATCH /{id}. Admin: GET /admin/tenants, /admin/audit.
Error shape: timestamp, status, code, message, path, requestId. Collections
paginated. Never expose secrets/stack traces.

## Database (Flyway V1–V9 + V100 demo seed; DB name `fixna`)
- V1 tenants/users: `tenants(id,name,tenant_type)`, `users(id,email UNIQUE,
  password_hash,first/last_name)`, `tenant_memberships(id,tenant_id,user_id,
  role, UNIQUE(tenant_id,user_id))` + indexes on user/tenant.
- V2 business: `businesses(id,tenant_id,name,category,description,website,
  phone)`, `business_locations(id,tenant_id,business_id,address,city,state,
  postal,country default India,lat,lng)`.
- V3 campaign: `campaigns(id,tenant_id,business_id,name,objective,status,
  total_budget>0,currency INR,start/end,external_reference)`,
  `campaign_offers(id,tenant_id,campaign_id,title,description,promo_code)`.
- V4 targeting/creative: `audiences(id,tenant_id,campaign_id,name,
  definition JSONB)`, `geo_targets(...,target_type,name,lat,lng,radius_km,
  country/region/city/postal)`, `creatives(...,channel,headline,body,cta,
  status default DRAFT)`, `campaign_channels(...,channel,allocated_budget≥0,
  UNIQUE(campaign_id,channel))`.
- V5 platform/analytics/leads: `platform_connections(...,platform,
  external_account_id,encrypted_access/refresh_token,status)`,
  `campaign_metrics(...,metric_date,spend,impressions,reach,clicks,
  conversions,leads, UNIQUE(campaign_id,metric_date))`,
  `leads(...,business_id,campaign_id?,name,phone,email,status,source)`.
- V6 ai/billing/audit: `ai_recommendations(...,campaign_id,type,
  prompt_version,provider,model,payload JSONB,status)`,
  `ai_usage(...,provider,model,request_type,input/output_tokens,
  estimated_cost,status)`, `subscriptions(tenant_id UNIQUE,plan_code,
  status,starts/ends)`, `audit_logs(id,tenant_id?,user_id?,action,
  resource_type,resource_id,metadata JSONB)`.
- V7–V9 (WF03/WF06/WF09): `refresh_tokens` (single-use rotation, stored
  hashes), `ai_usage_log` (usage/cost per recommendation), plan-limit/quota
  + audit additions.
- Seed V100: demo tenant `00000000-...-000001` (Urban Cuts SMB), business
  `...-000101` (Urban Cuts Salon, Noida Sec 18), location `...-000201`.
- Conventions: UUID PKs `gen_random_uuid()`, `timestamptz` timestamps,
  `tenant_id NOT NULL` + indexes on tenant-owned tables.

## AI engineering (ai/ + docs/06-ai/*)
Prompt file `ai/prompts/campaign-strategy.md` (v1, JSON-only, no side
effects). Schemas: `campaign-strategy` (objective,channels,reasoning,
confidence 0–1,assumptions,risks), `audience` (segments,reasoning), `budget-
allocation` (allocations[{channel,amount≥0}]), `creative` (headline,body,
cta). Eval cases: valid->schema-ok; missing budget rejected; negative
allocation rejected; sum>budget rejected; no autonomous-launch instruction;
low confidence shows uncertainty. Every LLM call needs versioned prompt +
schema + business validation + usage/cost row + timeout/rate-limit/quota.

## DevOps / tooling
- `docker-compose.yml` includes `infrastructure/docker/docker-compose.yml`
  (postgres 5432, redis 6379, mailhog 1025/8025).
- Scripts: `setup.sh` (cp .env + compose up), `start-dev.sh`/`stop-dev.sh`,
  `test-all.sh` (mvn test + npm test), `database/scripts/reset-db.sh`,
  `seed-db.sh` (backend Flyway runs migrations).
- CI `.github/workflows/ci.yml`: backend `mvn -f backend/pom.xml test`
  (Java 21 temurin), frontend npm install+build (node 22).
- Tests: 178 green (3 skipped = Testcontainers `FixnaEndToEndTest`
  auto-skip without Docker; full-journey suite via @DynamicPropertySource).
  Unit/integration suites are infrastructure-free.
- Infra: nginx placeholder; terraform README only (target cloud undecided).
- Dockerfiles: backend temurin:21-jre runs built jar; frontend node:22 build.
- Repo initialized: `d9bc6bb` (scaffold) + `cc95cc0` (WF03–WF14
  implementation, HEAD -> master as of 2026-09-15); `backend/target/`
  stale artifacts ignored.

## Constraints & quirks to remember
- PowerShell on Windows: `run_commands` output capture is flaky (truncation,
  "command may still be running", exit code 1 noise) — prefer `read_files`
  for inspection; run one shell command at a time when needed.
- RESOLVED: Flyway SQL is canonical at
  `backend/src/main/resources/db/migration/` (V1–V9 + V100 seed) and runs
  on boot; `database/migrations/` remains a docs copy that lags behind.
