# Current Task

Task: Backend foundation (Workflow 01) + Frontend foundation (Workflow 02)

Phase: 1-2
Status: IN_PROGRESS

Read:
- AGENTS.md
- .clinerules/*
- docs/02-architecture/*
- docs/03-api/api-overview.md
- requirements/*

Scope:
- Workflow 00 done: git init (uncommitted), Flyway copies under
  backend/src/main/resources/db/migration, backend smoke test green
  (mvn-test2.log BUILD SUCCESS), frontend skeleton builds (npm-build3.log).
- Workflow 01: common error envelope + FixnaException + GlobalExceptionHandler,
  RequestIdFilter, TenantContext, AuditEvent/Publisher, SecurityConfig
  (fail-closed), OpenApiConfig, HealthController, TenantContextTest,
  RequestIdAndErrorTest.
- Workflow 02: axios API client with envelope + request-id, QueryClient
  providers + loading/error states, layout wired, health probe on homepage.

Do not implement:
- real Google Ads
- real Meta Ads
- real WhatsApp
- payment provider
- autonomous AI actions
- JWT login flow (Workflow 03)
