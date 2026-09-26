# Deployment — Shared Demo (fixna.in)

**Stack:** Neon (DB) + **Render** (API) + **Vercel** (app) — no credit card required.

| Service | URL | Config |
|---------|-----|--------|
| Neon | PostgreSQL database `fixna` | Already set up |
| Render | `https://api.fixna.in` | `render.yaml` + dashboard env vars |
| Vercel | `https://app.fixna.in` | Root dir `frontend` |

---

## Quick checklist

- [ ] Code pushed to GitHub (`main` branch includes `render.yaml`)
- [ ] Render Blueprint deployed → health `UP` on `*.onrender.com`
- [ ] Render env vars set (Neon JDBC, JWT, CORS)
- [ ] Render custom domain `api.fixna.in` + DNS CNAME
- [ ] Vercel project connected → root `frontend`
- [ ] Vercel env: `NEXT_PUBLIC_API_BASE_URL=https://api.fixna.in/api`
- [ ] Vercel custom domain `app.fixna.in`
- [ ] Smoke test: register at `app.fixna.in`

---

## Architecture

```
Browser → app.fixna.in (Vercel)
              ↓
          api.fixna.in (Render, Docker + Spring Boot)
              ↓
          Neon PostgreSQL (fixna) — Flyway on startup
```

---

## CI/CD

| Trigger | Result |
|---------|--------|
| Push to `main` | Render auto-redeploys API (after Blueprint connected) |
| Push to `main` | Vercel auto-redeploys frontend (after repo connected) |
| Push/PR | GitHub `ci.yml` runs tests only |

No CLI or GitHub deploy secret needed for Render.

---

## Step 1 — Neon (done)

Database name: **`fixna`**.

JDBC URL format:

```
jdbc:postgresql://YOUR_NEON_HOST/fixna?sslmode=require
```

Get host/user/password from Neon → **Connection details** → JDBC.

---

## Step 2 — Render API

### 2.1 Deploy via Blueprint

1. [dashboard.render.com](https://dashboard.render.com) → sign up (no card).
2. **New** → **Blueprint**.
3. Connect your GitHub repo.
4. Render reads root `render.yaml` and creates **`fixna-localboost-api`** (Docker, free, Singapore).
5. Enter the **sync:false** variables when prompted (see `infrastructure/demo/render.env.example`):

| Variable | Value |
|----------|-------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://YOUR_NEON_HOST/fixna?sslmode=require` |
| `POSTGRES_USER` | e.g. `neondb_owner` |
| `POSTGRES_PASSWORD` | Neon password |
| `FIXNA_JWT_SECRET` | 48+ char random string |
| `FIXNA_CORS_ALLOWED_ORIGINS` | `https://app.fixna.in,https://fixna.in,https://fixna-localboost-api.onrender.com` |

Generate JWT (PowerShell):

```powershell
-join ((48..57 + 65..90 + 97..122) | Get-Random -Count 48 | ForEach-Object {[char]$_})
```

6. First deploy takes **5–10 minutes** (Maven build inside Docker).
7. After deploy, logs must show **`The following 1 profile is active: "staging"`** — not `No active profile set`.
   - `backend/Dockerfile` sets `SPRING_PROFILES_ACTIVE=staging` by default.
   - If you created the service **without** Blueprint, also add `SPRING_PROFILES_ACTIVE=staging` in Render → **Environment** (Blueprint env from `render.yaml` is not applied retroactively).
8. Test: `https://fixna-localboost-api.onrender.com/api/v1/health` → `status: UP` with component map, `version`, and `deployedAt`. Optional: set `FIXNA_DEPLOYED_AT` (ISO-8601 UTC) in Render env for an explicit last-deploy timestamp.

### 2.1b Demo user (manual SQL in Neon — not on app startup)

The app does **not** seed test data on staging. After Flyway has created tables, run once in **Neon SQL Editor**:

1. `tools/sql/neon-demo-seed.sql` — replace `REPLACE_WITH_BCRYPT_HASH`, then execute.
2. `tools/sql/neon-demo-data.sql` — campaigns, location, leads, metrics (idempotent).

Login: `owner@example.com` + the password you hashed.

Do **not** run `tools/sql/demo-data.sql` on Neon — it is psql-only for local `localboost` on localhost.

### 2.2 Custom domain `api.fixna.in`

Render → service → **Settings** → **Custom Domains** → add `api.fixna.in`.

DNS:

| Type | Name | Target |
|------|------|--------|
| CNAME | `api` | hostname shown by Render |

### 2.3 Free-tier notes

- Sleeps after **15 min** idle; first request after sleep ~30–60s.
- 512 MB RAM — JVM tuned in `render.yaml`.
- View logs: Render dashboard → **Logs**.

### 2.4 Update env vars later

Render → service → **Environment** → edit → **Save Changes** (triggers redeploy).

---

## Step 3 — Vercel frontend

1. [vercel.com/new](https://vercel.com/new) → import repo.
2. **Root Directory:** `frontend`
3. Environment variables (Production + Preview):

| Name | Value |
|------|-------|
| `NEXT_PUBLIC_API_BASE_URL` | `https://api.fixna.in/api` |
| `NEXT_PUBLIC_APP_ENV` | `demo` |

4. Deploy → **Settings** → **Domains** → add `app.fixna.in`
5. DNS: CNAME `app` → Vercel target

Optional: redirect `fixna.in` → `app.fixna.in` in Vercel or Cloudflare.

---

## Step 4 — Smoke test

1. `https://api.fixna.in/api/v1/health` — overall `UP`, components (`db`, `flyway`, `platform`, …), `version`, `deployedAt`
2. `https://app.fixna.in/register`
3. Register → business → campaign → AI recommendation (mock)

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Build fails on Render | Check **Logs** — usually JDBC or Docker build error |
| Health check fails | Verify Neon URL, user, password; check Flyway errors in logs |
| `502` / slow first request | Free tier waking from sleep — wait and retry |
| CORS error in browser | `FIXNA_CORS_ALLOWED_ORIGINS` must include exact `https://app.fixna.in` |
| Frontend can't reach API | `NEXT_PUBLIC_API_BASE_URL` must end with `/api` |
| SSL pending on `api.fixna.in` | Wait for DNS; verify CNAME in Render dashboard |

---

## Configuration reference

| Setting | Value | Where |
|---------|-------|-------|
| Spring profile | `staging` | `render.yaml` |
| AI / platform | `mock` | `render.yaml` |
| Redis | disabled | `application-staging.yml` |
| Server port | `PORT` (Render injects) | `application.yml` |

---

## Repo files

| File | Purpose |
|------|---------|
| `render.yaml` | Render Blueprint (API service) |
| `backend/Dockerfile` | Multi-stage Java 21 build |
| `infrastructure/demo/render.env.example` | Env var template |
| `infrastructure/demo/vercel.env.example` | Frontend env template |
| `frontend/vercel.json` | Vercel build settings |
| `tools/sql/neon-demo-seed.sql` | Demo user + tenant (Neon, once) |
| `tools/sql/neon-demo-data.sql` | Demo campaigns, leads, metrics (Neon) |
