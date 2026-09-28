# Project Context — Fixna LocalBoost

**Version:** 1.0.1 (2026-09-28)

## Product

**Fixna LocalBoost** (`fixna.in`) — multi-tenant SaaS for local businesses to define
promotions, geography, audience, budget; receive AI recommendations; approve campaigns;
execute via advertising platform adapters (mock in demo); monitor leads and metrics.

Initial market: India (Noida / Greater Noida / Delhi NCR).

## Architecture

- Modular monolith (Java 21, Spring Boot 3.x, PostgreSQL, Redis optional)
- Frontend: Next.js 16, React 19, TypeScript
- Source: GitHub org **fixna-in/fixna-localboost**
- Multi-tenancy: shared DB, `tenant_id` on tenant-owned rows; JWT + RBAC
- AI: `AIProvider` abstraction; mock provider in demo; output schema-validated
- Platforms: adapter pattern; mock Google/Meta/WhatsApp in demo

## Environment profiles

| Profile | Use |
|---------|-----|
| `local` | Developer machine, persistent Postgres, optional test-data seeder |
| `test` | CI/unit tests |
| `staging` | Shared demo (Render) — no auto seed, Redis off |
| `prod` | Production (fail-fast validators) |

## Non-negotiables (agents)

- Never trust client-supplied `tenantId` for authorization
- No secrets in logs; AI cannot spend money or launch campaigns autonomously
- Schema changes → new Flyway migration only
- Controllers are thin; business logic in services

## Brand

Unified mark: white **f** + lime **•** on `#163e32` rounded square.
Implementation: `frontend/src/brand/brand-mark-graphic.tsx` (favicon, header, auth hero).

## Observability

- `RequestIdFilter` — `X-Request-Id` + optional OTel `traceId`/`spanId` in MDC
- `GET /api/v1/health` — aggregated status, components, version, `deployedAt`
- Actuator `/actuator/health` — built-in probes including `flyway` (no custom flyway bean)

## Agent entry points

1. `AGENTS.md` — mission and standards
2. `.cursor/rules/` — persistent rules (`fixna-core`, `security-tenancy`, etc.)
3. `.cursor/tasks/CURRENT-TASK.md` — active work
4. `.cursor/memorybank/` — deployment and session context (this folder)
5. `.cursor/memorybank/chatgpt-handoff.md` — paste-ready project brief (no secrets)
6. `docs/07-operations/deployment.md` — operator runbook
