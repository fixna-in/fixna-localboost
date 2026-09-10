# System Patterns — Fixna LocalBoost

## Architecture decisions (docs/02-architecture/adr/*)
- ADR-001 Modular monolith for MVP; explicit module boundaries; extract
  services only when scale/team boundaries justify it.
- ADR-002 Shared PostgreSQL database + shared schema with `tenant_id` on
  tenant-owned records; design for future PostgreSQL RLS and dedicated
  schema/database for enterprise without changing domain APIs.
- ADR-003 `AIProvider` abstraction; `MockAIProvider` mandatory for local dev
  and tests; future OpenAI/Anthropic/Gemini providers behind the interface.
- ADR-004 `AdvertisingPlatformAdapter` isolates Google/Meta/WhatsApp specifics
  from campaign domain; MVP ships `MockGoogleAdsAdapter`,
  `MockMetaAdsAdapter`, `MockWhatsAppAdapter`.
- ADR-005 Human approval gate: AI recommendations are validated then require
  explicit user approval before execution (protects against autonomous spend).
- ADR-006 Identity/naming: brand Fixna, product LocalBoost, `fixna.in` naming
  in code/config/docs/Docker/CI; Java base package `in.fixna.platform`;
  subdomains `app.`/`api.`/`admin.`/`docs.`; DB name `fixna`.
- ADR-007 Java 21 LTS backend. ADR-008 PostgreSQL + Flyway. ADR-009 JWT
  access/refresh + server-side tenant membership + RBAC. ADR-010 Demo mode
  must run without external AI/advertising credentials.

## Backend module boundaries (docs/02-architecture/module-boundaries.md)
auth (identity/sessions), tenant (tenants/memberships), user (profile),
business (businesses/locations), campaign (lifecycle), audience (definitions),
geo (targets), creative (drafts/assets metadata), platform (connections +
adapters), analytics (metrics), lead (leads), ai (providers/recommendations/
usage), billing (plans/subscriptions/limits), notification (abstraction),
admin (platform administration), common (shared technical concerns:
exceptions, requestId/correlation, tenant context, audit, config, OpenAPI).

## Dependency direction (mandatory)
Controller -> Application Service -> Domain/Business Logic ->
Repository/Infrastructure. Controllers contain NO business logic — only HTTP
translation + validation. Domain code must never import provider SDKs; use
`AIProvider` and `AdvertisingPlatformAdapter` interfaces.

## Key flows
- Authenticated user -> JWT -> tenant membership lookup -> TenantContext ->
  tenant-aware repository (every query/command carries tenant scope).
- AI flow: campaign data -> prompt builder -> AIProvider -> structured JSON
  -> JSON schema validation -> business-rule validation -> platform
  compatibility check -> recommendation row -> user approval -> deterministic
  execution (never hold a DB tx while waiting on external calls).
- Launch flow: approved campaign -> QUEUED -> CREATING -> adapter calls with
  timeouts + safe retries + idempotency key -> ACTIVE; provider failures ->
  recoverable FAILED (retry/manual).
- Audit: every impactful action writes `audit_logs`; AI creation and approval
  audited separately.

## Security patterns (non-negotiable)
- Never trust client-supplied tenantId; resolve tenant from authenticated
  identity + membership.
- Every tenant-owned read/write enforces tenant scope; never load by ID alone.
- RBAC enforced server-side; UI auth is UX only.
- Tenant limits enforced server-side; provider tokens encrypted at rest;
  never log secrets/tokens/passwords/raw PII; tenant-scoped cache keys.
