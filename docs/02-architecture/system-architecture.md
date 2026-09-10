# System Architecture

```text
fixna.in
  |
app.fixna.in ---- Next.js
  |
api.fixna.in ---- Spring Boot modular monolith
  |
  +-- Auth/Tenant/RBAC
  +-- Business
  +-- Campaign
  +-- Audience/Geo
  +-- AI
  +-- Platform Adapters
  +-- Analytics/Leads
  +-- Billing/Admin
  |
PostgreSQL + Redis
```

The MVP uses a modular monolith. Modules have explicit boundaries.
External providers are adapter-based. AI is provider-agnostic.
