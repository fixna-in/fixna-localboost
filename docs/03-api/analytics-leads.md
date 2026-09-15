# Analytics & Leads API

Base `/api/v1`, Bearer JWT required. Tenant scope always derives from the
token — no tenant id in any payload. Windows are inclusive; `from`/`to`
default to the last 30 days (today − 29 … today).

## Analytics

- `GET /analytics/dashboard?from&to` → `{from, to, totals, campaigns, leads}`
  - `totals`: `{spend, impressions, reach, clicks, conversions, leads}`
  - `campaigns[]`: `{campaignId, name, totals}` (per-campaign rollup)
  - `leads`: `{newCount, contacted, qualified, converted, lost}` (current funnel snapshot)
- `GET /analytics/campaigns/{campaignId}?from&to` →
  `{campaignId, campaignName, from, to, totals, timeline[{metricDate, spend, impressions, reach, clicks, conversions, leads}]}`
  Cross-tenant campaign id → `404 CAMPAIGN_NOT_FOUND` (no leakage).
- `POST /analytics/campaigns/{campaignId}/metrics` → `202`
  Body `{metricDate, spend>=0, impressions, reach, clicks, conversions, leads}`.
  **Idempotent upsert** keyed on (campaign, metricDate) — re-posting a day
  overwrites instead of duplicating. Write roles only.
- `POST /analytics/demo-seed?campaignId={id}` → `200 {campaigns, metricDays}`
  Deterministic 14-day seed ending today; same values on every re-seed
  (idempotent demo data). `400 VALIDATION_FAILED` when `campaignId` is
  missing. Write roles only.

## Leads

- `POST /businesses/{businessId}/leads` → `201`
  Body `{name?, phone?, email?, campaignId?, source?}`; `campaignId` must
  belong to the caller's tenant → else `404 CAMPAIGN_NOT_FOUND`. Status starts
  `NEW`.
- `GET /leads?businessId&campaignId&status&page&size` →
  `{items[], page, size, totalElements, totalPages}` (size clamped to ≤100).
  Filters are tenant-scoped; campaign filter never bypasses business scope.
- `GET /leads/{id}` → `200` / cross-tenant → `404 LEAD_NOT_FOUND`
- `PATCH /leads/{id}/status` `{status}` → `200`. Funnel: NEW → CONTACTED →
  QUALIFIED → CONVERTED / LOST. `CONVERTED` and `LOST` are terminal:
  further changes → `409 LEAD_TERMINAL`. Write roles only.
- `DELETE /leads/{id}` → `204` (write roles only)

## Error envelope
All errors: `{timestamp, status, code, message, path, requestId}`.
Codes here: `BUSINESS_NOT_FOUND`, `CAMPAIGN_NOT_FOUND`, `LEAD_NOT_FOUND`,
`LEAD_TERMINAL`, `VALIDATION_FAILED`, `FORBIDDEN`.
