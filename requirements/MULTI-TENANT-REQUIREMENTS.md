# Multi-Tenant Requirements

## Model
A Tenant represents an isolated customer/account boundary.
A tenant can own multiple businesses.

## Isolation
Every tenant-owned entity has tenant_id.
Every query and command is tenant-scoped.
Authenticated membership determines tenant context.

## Tenant types
SMB, AGENCY, ENTERPRISE, INTERNAL.

## Roles
PLATFORM_ADMIN, TENANT_OWNER, TENANT_ADMIN, TENANT_MARKETING_MANAGER,
TENANT_MARKETING_USER, TENANT_VIEWER, AGENCY_ADMIN, AGENCY_USER.

## Security acceptance criteria
1. Tenant A cannot read Tenant B records.
2. Tenant A cannot update Tenant B records.
3. Tenant A cannot delete Tenant B records.
4. Changing tenantId in request parameters cannot bypass authorization.
5. Cache keys contain tenant scope.
6. Exports and analytics are tenant-scoped.
7. Audit records identify tenant scope.
8. Tests cover IDOR and cross-tenant access.

## Future
Support PostgreSQL Row-Level Security and optional dedicated database/schema
for enterprise tenants without changing domain APIs.
