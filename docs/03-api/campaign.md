# Campaign API

Base: /api/v1
All campaign endpoints require a Bearer access token; tenant scope is resolved
from the JWT (never from request parameters).

## Lifecycle

DRAFT -> READY_FOR_REVIEW -> APPROVED -> QUEUED -> CREATING -> ACTIVE ->
PAUSED/COMPLETED, with CREATING -> FAILED as the recoverable failure path.

- `POST /campaigns/{id}/launch` is the only way to reach QUEUED and is
  idempotent: APPROVED/FAILED -> QUEUED (request), QUEUED/CREATING/ACTIVE/PAUSED
  returns current state without error, anything else -> 409 NOT_APPROVED.

## Endpoints

### Campaigns
- `GET /campaigns?businessId=` — list current tenant's campaigns (optional
  business filter; cross-tenant businessId -> 404).
- `POST /campaigns` — create in DRAFT `{businessId, name, objective,
  totalBudget>0, currency?=INR, startAt?, endAt?}` -> 201. Error codes:
  `BUSINESS_NOT_FOUND` (unowned business), `INVALID_WINDOW`
  (end <= start), `FORBIDDEN` (viewer role).
- `GET /campaigns/{id}` — tenant-scoped detail (cross-tenant -> 404).
- `PUT /campaigns/{id}` — edit core fields, DRAFT only -> 409 `CAMPAIGN_LOCKED`.
- `DELETE /campaigns/{id}` — delete draft/review/failed/queue; approved or
  active -> 409 `CAMPAIGN_LOCKED`.
- `POST /campaigns/{id}/transitions` — `{to}` lifecycle transition, validated
  against the state machine -> 409 `ILLEGAL_TRANSITION` on invalid moves.
  QUEUED/CREATING cannot be requested here (orchestrator-only).
- `POST /campaigns/{id}/launch` — idempotent launch request (see lifecycle).

### Offers
- `GET /campaigns/{id}/offers` — list offers.
- `POST /campaigns/{id}/offers` — add `{title, description?, promoCode?}` -> 201.

### Channel budgets
- `GET /campaigns/{id}/channels` — list allocations.
- `PUT /campaigns/{id}/channels` — replace all `{channels: [{channel,
  allocatedBudget}]}`. DRAFT/READY_FOR_REVIEW only -> 409 `CAMPAIGN_LOCKED`.
  Duplicate channel names -> 400 `DUPLICATE_CHANNEL`; sum > total budget ->
  400 `BUDGET_EXCEEDED`.

## Error envelope
All errors: `{timestamp, status, code, message, path, requestId}` (see
docs/03-api/api-overview.md).