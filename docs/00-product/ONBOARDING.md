# Fixna LocalBoost — Onboarding Guide

This guide helps **platform operators**, **developers**, and **tenant users** (SMBs and agencies) get started with Fixna LocalBoost.

| Document | Audience | Contents |
|----------|----------|----------|
| [Client onboarding](./client-onboarding.md) | Operators, DevOps, developers | Environments, configuration, prerequisites, first-run setup |
| [Business flows](./business-flows.md) | Product, support, tenant users | End-to-end workflows for every feature |

## What LocalBoost is

Fixna LocalBoost (`fixna.in`) is a multi-tenant SaaS platform that helps local businesses in India (initial market: Noida / Greater Noida / Delhi NCR):

- Define **promotions**, **geography**, **audience**, and **budget**
- Receive **AI recommendations** (advisory only — never auto-executed)
- Generate and review **creatives**
- **Approve** campaigns before any spend
- Execute through **advertising platform adapters** (Google, Meta, WhatsApp — mock in MVP)
- Monitor **metrics** and manage **leads**

LocalBoost is an **orchestration and intelligence layer**. It does not replace Google Ads, Meta Ads, or WhatsApp.

## Architecture at a glance

| Layer | Technology |
|-------|------------|
| Frontend | Next.js, React, TypeScript (`app.fixna.in`) |
| API | Java 21, Spring Boot 3 (`api.fixna.in`) |
| Database | PostgreSQL + Flyway migrations |
| Cache | Redis (optional in MVP) |
| AI | `AIProvider` abstraction (mock default) |
| Ads | `AdvertisingPlatformAdapter` (mock default) |

## Tenancy model

```
Tenant → Users (memberships + RBAC) → Businesses → Locations
                                              → Campaigns → Geo / Audience / Creatives / Channels
                                                          → Launch → Metrics / Leads
```

- Every tenant-owned record carries `tenant_id`.
- **Never** send `tenantId` from the browser for authorization — scope always comes from the JWT.
- Platform admins belong to an **INTERNAL** tenant and use separate `/admin/*` endpoints.

## Quick start paths

| Goal | Path |
|------|------|
| One-command local demo (Windows, no Docker) | `tools/start-local-demo.cmd` — see [README](../../README.md) |
| Persistent local PostgreSQL | `application-local` profile + `tools/sql/schema.sql` + demo data |
| **Shared demo on fixna.in** | [Deployment guide](../07-operations/deployment.md) — Neon + Render + Vercel |
| API exploration | `http://localhost:8080/swagger-ui.html` after backend start |
| Frontend | `cd frontend && npm ci && npm run dev` with `frontend/.env.local` |

## Related documentation

- Requirements: `requirements/USER-STORIES.md`, `requirements/FEATURE-MATRIX.md`
- API contracts: `docs/03-api/`
- Architecture: `docs/02-architecture/`
- Deployment: `docs/07-operations/deployment.md`
- Security: `docs/07-operations/security-hardening.md`
- Agent rules: `AGENTS.md`, `.cursor/rules/`
