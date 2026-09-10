# Multi-Tenancy Rules

Tenancy is first-class.

Model:
Tenant -> Users/Memberships -> Businesses -> Locations/Campaigns/etc.

MVP strategy:
shared PostgreSQL database, shared schema, tenant_id on tenant-owned records.

Tenant types:
SMB, AGENCY, ENTERPRISE, INTERNAL.

Roles:
PLATFORM_ADMIN, TENANT_OWNER, TENANT_ADMIN, TENANT_MARKETING_MANAGER,
TENANT_MARKETING_USER, TENANT_VIEWER, AGENCY_ADMIN, AGENCY_USER.

Rules:
- Never trust client tenantId.
- Repository methods must be tenant-aware.
- Never load tenant-owned data by ID alone.
- Verify both tenant ownership and parent resource ownership.
- Design for future PostgreSQL Row-Level Security.
- Prevent cross-tenant leakage in logs, cache keys, metrics and exports.
- Cache keys must include tenant identity.
