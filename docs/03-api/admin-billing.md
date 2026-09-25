# Admin, Billing & Notifications API (Workflow 09)

Base: /api/v1 — Bearer required. Tenant scope from JWT only, never from
query/path parameters. Platform admin endpoints are gated by INTERNAL tenant
membership (separate from tenant RBAC; PLATFORM_ADMIN role is not stored as a
membership role).

## Subscriptions (any signed-in tenant member)

`GET /subscriptions/current` -> 200
```json
{
  "tenantId": "uuid",
  "planCode": "FREE|STARTER|GROWTH",
  "status": "ACTIVE",
  "effectiveLimits": { "maxBusinesses": 1, "maxCampaigns": 1, "maxAiRequestsPerDay": 10 }
}
```
Missing subscriptions materialize lazily as FREE. No payment instructions —
payment provider integration is explicitly excluded for MVP.

## Audit log viewer

`GET /audit?action=&page=&size=` -> 200 page of the caller's own tenant audit
events, newest first (`AuditEventView[]` + `page/size/totalElements`).

`GET /admin/audit?tenantId=&action=&page=&size=` -> same shape, any tenant.
INTERNAL platform admins only, else 403 `FORBIDDEN`.

## Platform admin (INTERNAL tenant only)

- `GET /admin/tenants?page=&size=` -> `[{id, name}]`
- `GET /admin/tenants/{tenantId}/members` -> `[{userId, email, role}]`
- `PUT /admin/tenants/{tenantId}/plan` body `{"planCode":"STARTER"}` ->
  `{tenantId, planCode, status}`. Unknown codes -> 400 `INVALID_PLAN_CODE`.

Errors: 403 `FORBIDDEN` (non-INTERNAL caller), 400 `TENANT_REQUIRED`
(missing tenantId).

## Enforced limits

- `POST /businesses` -> 403 `PLAN_BUSINESS_LIMIT` when the tenant owns at
  least `maxBusinesses` (counted per plan).
- `POST /campaigns` -> 403 `PLAN_CAMPAIGN_LIMIT` when at `maxCampaigns`.
- `POST /ai/recommendations` -> 403 `AI_QUOTA_EXCEEDED` when the tenant
  already used `maxAiRequestsPerDay` rows in the trailing 24h window (checked
  before the provider call).

## Notifications

Outbound events are emitted through the `NotificationProvider` abstraction
(logging provider in MVP) on:
- `lead.created` (LeadService after create)
- `campaign.launched` / `campaign.launch_failed` (launch orchestrator after
  the terminal outcome is committed)

Events carry only ids and status metadata — never secrets, tokens, passwords
or raw customer PII.

## Error envelope
All errors: `{timestamp, status, code, message, path, requestId}` (see
docs/03-api/api-overview.md).