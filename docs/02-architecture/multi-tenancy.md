# Multi-Tenancy Architecture

Strategy: shared database/shared schema with tenant_id.

Authenticated user -> tenant membership -> TenantContext -> tenant-aware repository.

Every tenant-owned query includes tenant scope. Future PostgreSQL RLS is supported
as a defense-in-depth option. Enterprise dedicated schemas/databases are future
deployment options without changing domain APIs.
