# Current Task

Task: Post-1.0.1 maintenance
Phase: production / demo
Status: COMPLETE (1.0.1 — 2026-09-28)

## Release 1.0.1 (2026-09-28)

- [x] Aggregated `/api/v1/health` (components, version, deployedAt)
- [x] Fix Render startup (`FlywayHealthIndicator` bean conflict removed)
- [x] Next.js 16.3.6; CI dependency-review on PRs only
- [x] GitHub org **fixna-in** — Render + Vercel reconnected and deploying
- [x] Local launcher `mvn clean compile`; Flyway 11.20 for PostgreSQL 18
- [x] Changelog + memorybank updated

## Live URLs

- https://app.fixna.in
- https://api.fixna.in/api/v1/health

## Next (1.1.0+ backlog)

- [ ] E2E smoke on production URLs (login → dashboard → campaigns)
- [ ] Vitest/Playwright frontend tests
- [ ] OpenAPI generation from springdoc
