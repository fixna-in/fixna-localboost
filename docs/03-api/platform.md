# Platform Adapters & Connections API
Base: /api/v1 — Bearer required. Tenant scope from JWT only.

## Platform connections
GET /platform-connections -> 200 [{platform, externalAccountId, status, createdAt}]
POST /platform-connections {platform=GOOGLE|META|WHATSAPP, externalAccountId?} -> 201 (mock mode: no credentials accepted or stored)
DELETE /platform-connections/{platform} -> 204 (clears token columns)

Errors: 400 UNSUPPORTED_PLATFORM, 409 ALREADY_CONNECTED, 404 CONNECTION_NOT_FOUND, 403 FORBIDDEN (viewer role).

## Launch execution
POST /campaigns/{id}/execute-launch -> 200 CampaignResponse
Executes QUEUED -> CREATING -> ACTIVE | FAILED through the platform adapters
(one adapter call per channel allocation). Idempotent: ACTIVE/PAUSED campaigns
return unchanged with zero adapter calls; external ids derive from
external_reference (BR-6) so retries never duplicate platform campaigns.
Retryable provider failures retry up to 3 attempts then map to
PROVIDER_RETRY_EXHAUSTED; permanent failures map to PROVIDER_REJECTED.
Errors: 404 CAMPAIGN_NOT_FOUND (cross-tenant ids included — no leakage),
409 CAMPAIGN_NOT_IN_QUEUE (not QUEUED/CREATING).

## Safety (per platform integration rules)
- Adapters sit behind AdvertisingPlatformAdapter + registry; no provider SDK
  types in campaign/business domain (ADR-004).
- DB transactions never span adapter calls: begin/applyOutcome state edges are
  separate transactional beans from the non-transactional coordinator.
- Token columns on platform_connections are write-only server-side; never
  returned by any endpoint and never logged.
- Every connect/disconnect and launch edge writes an audit event.
