# Geo & Audience API

Base: /api/v1 — all endpoints require a Bearer token; tenant scope resolves
from the JWT, never from request parameters. Campaign ids are verified
tenant-scoped (cross-tenant -> 404 `CAMPAIGN_NOT_FOUND`).

## Geo targets (US-007/US-008)
`/api/v1/campaigns/{campaignId}/geo-targets`

- `GET` — list targets of the (owned) campaign.
- `POST` — add target. Payload:
  `{targetType, name?, latitude?, longitude?, radiusKm?, countryCode?,
  regionCode?, city?, postalCode?}`
  Exactly one dimension must be populated for the declared type:
  - `RADIUS` -> latitude + longitude + radiusKm (> 0)
  - `CITY` -> city
  - `POSTAL` -> postalCode
  - `REGION` -> regionCode
  - `COUNTRY` -> countryCode
  Any violation -> 400 `INVALID_GEO_TARGET`. Viewer role -> 403.
- `PUT /{targetId}` / `DELETE /{targetId}` — update/remove; target must belong
  to the same campaign -> 400 `GEO_TARGET_MISMATCH`. Viewer role -> 403.

## Audiences (US-008)
`/api/v1/campaigns/{campaignId}/audiences`

- `GET` — list audience definitions of the campaign.
- `POST` — create `{name, definition}`. `definition` is a JSON object with
  recognized criteria only: `ageMin`, `ageMax` (numbers, 0 <= ageMin <=
  ageMax) and `genders`, `interests`, `incomeBrackets`, `deviceTypes`,
  `languages` (non-empty arrays of non-blank strings). Unknown keys,
  inverted age ranges or empty arrays -> 400 `INVALID_AUDIENCE`.
- `PUT /{audienceId}` / `DELETE /{audienceId}` — update/delete; audience must
  belong to the same campaign -> 400 `AUDIENCE_MISMATCH`.

## Errors
All errors use the standard envelope `{timestamp, status, code, message,
path, requestId}` (see docs/03-api/api-overview.md).

## Frontend note
The campaign wizard uses a static/fallback map UI when no map provider is
configured (workflow 05, docs/05-integrations).