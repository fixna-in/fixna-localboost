# Fixna LocalBoost API
Base: /api/v1
Errors: {timestamp, status, code, message, path, requestId}

## Auth (public)
POST /auth/register {email, password>=8, firstName?, lastName?, tenantName} -> 201 {accessToken, refreshToken, tokenType, expiresInSeconds, userId, tenantId, role}
POST /auth/login {email, password} -> 200 (tenant selected server-side from memberships)
POST /auth/refresh {refreshToken} -> 200 (single-use rotation)
POST /auth/logout -> 204 (revokes all refresh tokens; Bearer required)

## Tenants (Bearer required; scope from JWT, never params)
GET /tenants/current -> 200 {id, name, tenantType}
POST /tenants {name} -> 201 (caller becomes TENANT_OWNER)
GET /tenants/current/members -> 200 [{userId, email, role}] (admin-only)
DELETE /tenants/current/members/{userId} -> 204 (admin-only; last owner protected)

## Auth throttling
POST /auth/* is rate limited per client IP (default 20/min). Over quota ->
429 `RATE_LIMIT_EXCEEDED` with `Retry-After` seconds; the response keeps the
standard error envelope.

## Businesses (Bearer required; all lookups tenant-scoped)
GET /businesses -> 200 [...] (current tenant only)
POST /businesses {name, category?, description?, websiteUrl?, phone?} -> 201
GET /businesses/{id} -> 200 (cross-tenant id -> 404, no leakage)
PUT /businesses/{id} -> 200 (write roles only)
DELETE /businesses/{id} -> 204 (write roles only)
GET /businesses/{id}/locations -> 200 [...]
POST /businesses/{id}/locations {addressLine?, city?, state?, postalCode?, country?=India, latitude?, longitude?} -> 201

## RBAC
Write (POST/PUT/DELETE on tenants/businesses): TENANT_OWNER, TENANT_ADMIN, TENANT_MARKETING_MANAGER, TENANT_MARKETING_USER, AGENCY_*.
Read: all member roles. TENANT_VIEWER is read-only (403 on writes).
Membership admin (list/remove members): canAdminister roles (OWNER/ADMIN/AGENCY_ADMIN).
