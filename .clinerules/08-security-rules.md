# Security Rules

Authentication: JWT access/refresh tokens.
Password hashing: BCrypt or Argon2.
Authorization: RBAC.
Never trust tenantId from request payload/query as authorization.
Resolve tenant from authenticated identity and tenant membership.
Every tenant-owned read/write must enforce tenant scope.
Encrypt sensitive provider tokens at rest.
Use secure headers, CORS allowlists, rate limits and audit logs.
