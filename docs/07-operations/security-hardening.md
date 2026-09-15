# Security Hardening (Workflow 10)

Fail-closed baseline: every tenant-owned read/write resolves scope from the
JWT-derived `TenantContext` — never from client parameters; cross-tenant ids
surface as 404 (same code as missing ids, no existence leak).

## Controls implemented

- **Authentication**: JWT access (15m) + refresh (7d); refresh hashes stored
  (SHA-256), single-use rotation, revocation on logout. `SecurityStartupValidator`
  refuses `prod` boot on a missing/dev/weak `FIXNA_JWT_SECRET`.
- **Authorization**: RBAC via membership roles; viewer is read-only;
  membership admin requires `canAdminister`; platform admin requires an
  INTERNAL tenant (never a client parameter).
- **Tenant isolation / IDOR**: all owned lookups via `findByIdAndTenantId`
  (parent + child re-verified, e.g. geo/audience campaign bindings).
  `RepositoryTenantScopeSweepTest` fails any new repository without a
  `TenantId`-scoped query; `CrossTenantSweepTest` asserts 404 on cross-tenant
  ids for geo/lead/campaign paths.
- **Validation**: DTO records validated at the edge (`@Valid`); request-side
  business rules run before persistence/provider calls; repository storage
  holds only validated payloads.
- **CORS**: exact-origin allowlist from `FIXNA_CORS_ALLOWED_ORIGINS`
  (dev default: local Next.js origins). Empty list refuses cross-origin calls.
  Credentials allowed; `Authorization`, `Content-Type`, `X-Request-Id` headers.
- **Rate limiting**: per-IP+route sliding window (60s) on `/api/v1/auth/**`
  via `RateLimiter`/`RateLimitFilter`. Over quota → 429 `RATE_LIMIT_EXCEEDED`
  envelope + `Retry-After` header. Default 20/min/IP
  (`FIXNA_RATE_LIMIT_PER_IP_PER_MINUTE`). Audit log emits masked IP
  (IPv4 last octet / IPv6 tail masked).
- **Secrets**: BCrypt password hashes; refresh tokens stored hashed; tokens
  never logged, never in error bodies or query-echoing paths; `getRequestURI`
  (never query string) in envelopes.
- **Logs**: `fixna.error` receives unexpected Throwables server-side with
  requestId/path correlation; callers only see the generic INTERNAL_ERROR.
- **Headers**: `nosniff`, `DENY` framing, strict referrer policy,
  `X-XSS-Protection: 0`, `no-store` on `/api/v1` (JWT-bearing responses).
  HSTS opt-in via `FIXNA_HSTS_ENABLED` on TLS-terminated hosts only. No global
  CSP by design (would break self-served Swagger UI; the API returns JSON).
- **SQL injection**: JPA derived queries only — no dynamic JPQL/SQL string
  building anywhere (verified by sweep; persistence review per PR).
- **CSRF**: disabled cookies-by-design (Bearer API, stateless, no sessions).
- **Dependencies**: no new runtime dependencies in this workflow; run the
  GitHub dependency-review gate on PRs and rotate `FIXNA_JWT_SECRET` on
  schedule via deployment secrets (never committed).

## X-Forwarded-For trust note

`RateLimitFilter` honors the left-most `X-Forwarded-For` IP under the
assumption that the app sits behind the platform reverse proxy that sets it.
Client-supplied XFF spoofing is irrelevant here because (a) the limit is a
mitigation layer, not the credential control, and (b) excess 429s from a
spoofed IP only punish the spoofer's own key. Do not rely on XFF for
authorization.

## Error envelope
All errors: `{timestamp, status, code, message, path, requestId}`. Unexpected
failures: code `INTERNAL_ERROR`, message `An unexpected error occurred`.