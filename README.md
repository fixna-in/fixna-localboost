# Fixna Platform

Fixna is the platform brand. **Fixna LocalBoost** is the first product: an AI-assisted,
multi-tenant local advertising orchestration platform for SMBs.

## Domains
- `https://fixna.in` — brand/marketing site
- `https://app.fixna.in` — application
- `https://api.fixna.in` — API
- `https://admin.fixna.in` — administration (future)
- `https://docs.fixna.in` — documentation (future)

## Architecture
- Frontend: Next.js + React + TypeScript
- Backend: Java 21 + Spring Boot 3.x
- Database: PostgreSQL + Flyway
- Cache: Redis
- AI: provider abstraction with mock provider for local development
- External advertising: adapter pattern for Google Ads, Meta Ads and WhatsApp
- Tenancy: shared PostgreSQL schema with mandatory `tenant_id`, designed for future RLS

## Local development
1. Copy `.env.example` to `.env`
2. Start infrastructure with `docker compose up -d`
3. Start backend and frontend using their project instructions.
4. Demo mode works without external advertising or LLM credentials.

See `AGENTS.md` and `.clinerules/` before using an AI coding agent.
