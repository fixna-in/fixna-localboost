# Project Brief — Fixna LocalBoost

## What this is
Fixna is the platform brand (domain `fixna.in`). **Fixna LocalBoost** is the first
product: an AI-assisted, multi-tenant SaaS platform that helps local businesses
(SMBs) in India — starting with Noida/Greater Noida/Delhi NCR — plan, launch and
measure geographically targeted advertising promotions across channels
(Google Ads, Meta Ads, WhatsApp), with AI-generated recommendations that always
require human approval before any execution.

LocalBoost is an **orchestration/intelligence layer**, not a replacement for
Google Ads, Meta Ads or WhatsApp.

## Initial market & categories
- Market: India, starting with Noida/Greater Noida/Delhi NCR.
- Categories: restaurants, cafes, salons, gyms, clinics, coaching institutes,
  retailers, home services, automotive services.

## Core user journey (E2E, must always work in demo mode)
register -> tenant -> business -> location -> campaign -> offer -> geography ->
audience -> budget -> AI recommendation -> creatives -> review -> approval ->
mock launch (Google/Meta/WhatsApp adapters) -> metrics/leads.

## MVP scope (from requirements/PRD.md, MVP-REQUIREMENTS.md)
Authentication, multi-tenancy, RBAC, business/location management, campaign
CRUD + lifecycle, geo/audience targeting, budget validation, AI recommendations
(strategy/audience/budget/creative), user approval workflow, mock platform
adapters (Google/Meta/WhatsApp — no real credentials), analytics/metrics,
leads, admin foundation, audit logging, OpenAPI docs, automated tests, Docker
local environment, observability foundation.

## Explicit non-goals (MVP)
Autonomous ad spending, full CRM, advanced attribution, mobile apps, real
payment provider integration, real advertising provider credentials/production
API calls.

## Success metrics
Activation, campaigns created, approved/launchable campaigns, lead conversion,
cost-per-lead visibility, tenant retention, AI recommendation acceptance rate.

## Repository status (as of last review)
**Phase 0 — bootstrap/scaffold stage.** No git repository initialized yet.
No business logic implemented. Only:
- `backend/src/main/java/in/fixna/platform/FixnaApplication.java` (empty
  `@SpringBootApplication` bootstrap).
- Per-module `README.md` placeholders under
  `backend/src/main/java/in/fixna/platform/{auth,tenant,user,business,campaign,
  audience,geo,creative,platform,analytics,lead,ai,billing,notification,admin,common}/`
  each containing only: "Implement according to requirements, architecture and
  Cline rules."
- No test sources exist yet (`backend/src/test` is empty).
- Flyway SQL migrations (V1–V6) already fully define the MVP schema (see
  `databaseSchema.md`) but the app is configured with `ddl-auto: validate`
  (Hibernate does not create schema — Flyway must run first).
- `frontend/` has only `package.json` + `Dockerfile`; `frontend/src` has a
  `.gitkeep` only — no Next.js app code yet.
- `.cline/tasks/CURRENT-TASK.md` states: Task = "Project bootstrap", Phase 0,
  Status = NOT_STARTED. Do NOT implement real Google Ads/Meta Ads/WhatsApp,
  payment provider, or autonomous AI actions in this phase.

See `progress.md` for what remains and `activeContext.md` for current focus.
