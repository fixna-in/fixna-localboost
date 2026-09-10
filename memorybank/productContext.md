# Product Context — Fixna LocalBoost

## Why this exists
Local businesses (SMBs) struggle to run effective hyperlocal digital ads:
they lack marketing expertise, budget guidance, creative skills and a simple
way to coordinate Google/Meta/WhatsApp channels. LocalBoost gives them a
guided wizard: define promotion + geography + audience + budget, receive AI
recommendations, review creatives, approve, then launch through connected
advertising platforms (mocked in MVP) and monitor leads/metrics.

## Users & roles
- Business owners, marketing users, agencies, platform administrators.
- Tenant types: SMB, AGENCY, ENTERPRISE, INTERNAL.
- Roles: PLATFORM_ADMIN, TENANT_OWNER, TENANT_ADMIN, TENANT_MARKETING_MANAGER,
  TENANT_MARKETING_USER, TENANT_VIEWER, AGENCY_ADMIN, AGENCY_USER.

Tenancy model: Tenant -> Users/Memberships -> Businesses -> Locations /
Campaigns / etc. A tenant can own multiple businesses.

## User stories (requirements/USER-STORIES.md)
US-001 register; US-002 create tenant; US-003 invite/manage users;
US-004 create business; US-005 add locations; US-006 create campaign;
US-007 select geographic area; US-008 define audience; US-009 budget+duration;
US-010 request AI recommendations; US-011 review creatives; US-012 approve;
US-013 mock launch; US-014 view metrics; US-015 manage leads; US-016/017 admin
inspect tenants/audit; US-018 tenant sees AI usage vs limits.

## Feature matrix (MVP vs later)
MVP (phases 1-6): auth, multi-tenancy, RBAC, business/locations, campaigns,
geo/audience, AI recommendations, creatives, mock Google/Meta/WhatsApp,
analytics, leads, admin, billing foundation.
Later (phase 7+): real ad APIs, real payment integration.

## Product rules that shape UX
- AI is advisory only: recommendations displayed with reasoning, assumptions,
  confidence, risks; user must explicitly approve before any launch.
- Demo mode: the entire core journey works with zero external credentials
  (MockAIProvider + Mock*Ads adapters).
- Campaign lifecycle is linear with a failure branch:
  DRAFT -> READY_FOR_REVIEW -> APPROVED -> QUEUED -> CREATING -> ACTIVE ->
  PAUSED/COMPLETED; CREATING -> FAILED -> retry/manual.
- Budget integrity: total budget positive; channel allocations sum ≤ total.
- Launch idempotency: repeating the same launch request must not duplicate
  external campaigns (idempotency key / external_reference).
- Every impactful action creates an audit event visible to admins.

## UX surface (planned, from API overview)
Auth pages (register/login), tenant dashboard, business/location management,
campaign wizard (offer -> geo -> audience -> budget -> AI -> creatives ->
review -> approve -> launch), campaign metrics view, leads board, admin
tenants/audit views.
