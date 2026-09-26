# Deployment State — Shared Demo

**Last updated:** 2026-09-26  
**Release:** 1.0.0

## Status

| Component | Status |
|-----------|--------|
| Neon database `fixna` | ✅ Provisioned |
| Flyway migrations | ✅ Applied on first API deploy |
| Render API | ✅ Live — health `UP` |
| Demo user seed | ✅ Operator-loaded via SQL |
| Demo campaign data | ✅ `neon-demo-data.sql` (operator) |
| Custom domain `api.fixna.in` | ✅ CNAME active |
| Vercel frontend | ✅ Live at https://app.fixna.in |
| Custom domain `app.fixna.in` | ✅ CNAME active |

## Render service

- **Name:** `fixna-localboost` (hostname `fixna-localboost.onrender.com`)
- **Blueprint:** root `render.yaml`
- **Build:** `backend/Dockerfile` → `fixna-api.jar`
- **Profile:** `staging` via Dockerfile `ENV SPRING_PROFILES_ACTIVE=staging`
- **Health check:** `/actuator/health`

## DNS CNAME targets (operator copies from dashboards)

| Subdomain | Type | Target source |
|-----------|------|---------------|
| `api` | CNAME | Render → Custom Domains → `api.fixna.in` |
| `app` | CNAME | Vercel → Domains → `app.fixna.in` (often `cname.vercel-dns.com`) |

## SQL scripts (Neon SQL Editor only)

| File | When | Idempotent |
|------|------|------------|
| `tools/sql/neon-demo-seed.sql` | Once, before demo data | Skips if `owner@example.com` exists |
| `tools/sql/neon-demo-data.sql` | After seed | Skips if demo campaigns exist |
| `tools/sql/demo-data.sql` | **Local psql only** — do not run on Neon |
| `tools/sql/schema.sql` | **Local psql only** — empty `localboost` DB |

## Troubleshooting quick reference

| Symptom | Fix |
|---------|-----|
| `No active profile set` | Set `SPRING_PROFILES_ACTIVE=staging` on Render or redeploy Dockerfile |
| Generated security password in logs | Redeploy with `UserDetailsServiceAutoConfiguration` excluded |
| Redis health spam | Use `staging` profile + `management.health.redis.enabled: false` |
| CORS browser error | Add exact frontend URL to `FIXNA_CORS_ALLOWED_ORIGINS` |
| Frontend calls localhost | Redeploy Vercel after setting `NEXT_PUBLIC_API_BASE_URL` |

## Files

- `render.yaml` — Render Blueprint
- `backend/Dockerfile` — API container image
- `infrastructure/demo/render.env.example` — Render env template
- `infrastructure/demo/vercel.env.example` — Vercel env template
- `frontend/vercel.json` — Vercel build settings
- `frontend/src/brand/brand-mark-graphic.tsx` — canonical logo/favicon artwork
- `docs/07-operations/deployment.md` — full guide
- `.cursor/memorybank/chatgpt-handoff.md` — no-secrets external handoff
