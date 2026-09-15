# API Overview

Base: /api/v1

Auth:
POST /auth/register
POST /auth/login
POST /auth/refresh
POST /auth/logout

Tenant:
GET /tenants/current
POST /tenants
GET /tenants/current/members

Business:
GET/POST /businesses
GET/PATCH/DELETE /businesses/{id}

Campaign:
GET/POST /campaigns
GET/PATCH/DELETE /campaigns/{id}
POST /campaigns/{id}/recommendation
POST /campaigns/{id}/approve
POST /campaigns/{id}/launch
POST /campaigns/{id}/pause
POST /campaigns/{id}/resume
GET /campaigns/{id}/metrics

Leads:
GET/POST /leads
PATCH /leads/{id}

Admin:
GET /admin/tenants
GET /admin/audit
GET /admin/tenants/{id}/members
PUT /admin/tenants/{id}/plan

Subscriptions:
GET /subscriptions/current

Audit viewer:
GET /audit

Details: docs/03-api/analytics-leads.md, .../campaign.md, .../geo-audience.md,
.../ai.md, .../platform.md, .../admin-billing.md
