# Deployment State — Shared Demo

**Last updated:** 2026-09-28  
**Release:** 1.0.1

## Status

| Component | Status |
|-----------|--------|
| GitHub org `fixna-in` | ✅ Repo connected |
| Neon database `fixna` | ✅ Provisioned |
| Flyway migrations | ✅ Applied (V1–V9) |
| Render API | ✅ Live — `/api/v1/health` UP |
| Vercel frontend | ✅ Live at https://app.fixna.in |
| Custom domain `api.fixna.in` | ✅ CNAME active |
| Custom domain `app.fixna.in` | ✅ CNAME active |
| GitHub Actions CI | ✅ Push/PR on `main`/`develop` |
| Demo user + data | ✅ Operator-loaded via Neon SQL |

## GitHub

- **Org:** https://github.com/fixna-in
- **Repo:** `fixna-in/fixna-localboost`
- **Deploy branch:** `main` (confirm in Render/Vercel dashboards)
- **CI:** `.github/workflows/ci.yml` (backend test + frontend build + PR dependency-review)

## Render service

- **Name:** `fixna-localboost-api` (Blueprint from `render.yaml`)
- **Build:** `backend/Dockerfile` → `fixna-api.jar`
- **Profile:** `staging` (`ENV SPRING_PROFILES_ACTIVE=staging`)
- **Health:** `GET /api/v1/health` (aggregated) or `/actuator/health`
- **Reconnect after org move:** Render dashboard → GitHub app → grant `fixna-in` access → link repo

## Vercel

- **Root directory:** `frontend`
- **Framework:** Next.js 16.3.6
- **Production URL:** https://app.fixna.in
- **Note:** Hobby plan cannot Git-link **private org** repos — use public repo, Pro plan, or CLI/Actions deploy

## DNS CNAME targets

| Subdomain | Type | Target source |
|-----------|------|---------------|
| `api` | CNAME | Render → Custom Domains |
| `app` | CNAME | Vercel → Domains |

## SQL scripts (Neon SQL Editor only)

| File | When | Idempotent |
|------|------|------------|
| `tools/sql/neon-demo-seed.sql` | Once, before demo data | Skips if `owner@example.com` exists |
| `tools/sql/neon-demo-data.sql` | After seed | Skips if demo campaigns exist |
| `tools/sql/demo-data.sql` | **Local psql only** |
| `tools/sql/schema.sql` | **Local psql only** |

## Troubleshooting quick reference

| Symptom | Fix |
|---------|-----|
| Deploys not triggering after org move | Reconnect Render/Vercel to `fixna-in/fixna-localboost`; grant org GitHub app access |
| Vercel Hobby + private org repo | Public repo, upgrade Pro, or deploy via `vercel --prod` / Actions |
| `flyway` bean conflict on startup | Do not use `@Component("flyway")` custom health indicator |
| `Lookup method resolution failed` locally | `mvn -f backend/pom.xml clean compile` or use updated launcher |
| `No active profile set` on Render | `SPRING_PROFILES_ACTIVE=staging` |
| CORS browser error | Add `https://app.fixna.in` to `FIXNA_CORS_ALLOWED_ORIGINS` |
| Port 3000/8080 in use (local) | Stop old `node.exe`/`java.exe` or `-FrontendPort 3001 -BackendPort 18081` |

## Files

- `render.yaml` — Render Blueprint
- `backend/Dockerfile` — API container image
- `infrastructure/demo/render.env.example` — Render env template
- `infrastructure/demo/vercel.env.example` — Vercel env template
- `frontend/vercel.json` — Vercel build settings
- `docs/07-operations/deployment.md` — full guide
