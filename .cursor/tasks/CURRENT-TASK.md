# Current Task

Task: Shared demo deployment (fixna.in)
Phase: production / demo
Status: IN_PROGRESS

## Completed (2026-09-26)
- [x] Render API deployed and healthy (`fixna-localboost.onrender.com`)
- [x] Neon database `fixna` + Flyway migrations
- [x] Staging profile: no Redis, no test-data seed, no Spring default password
- [x] `render.yaml`, `backend/Dockerfile`, deployment docs
- [x] Neon SQL: `neon-demo-seed.sql` + `neon-demo-data.sql`
- [x] Removed Fly.io and other unused deploy artifacts
- [x] Memory bank + changelog updated

## In progress (operator)
- [ ] Vercel frontend deploy (`frontend/`, env vars)
- [ ] DNS CNAME: `api.fixna.in` → Render, `app.fixna.in` → Vercel
- [ ] Render `FIXNA_CORS_ALLOWED_ORIGINS` includes frontend URL
- [ ] End-to-end smoke test on custom domains

## Next (engineering backlog)
- [ ] CI: Docker image build for backend
- [ ] CI: test coverage reporting
- [ ] CI: container image security scan
- [ ] Wire Vitest for frontend unit tests
- [ ] Generate `docs/03-api/openapi.yaml` from springdoc
