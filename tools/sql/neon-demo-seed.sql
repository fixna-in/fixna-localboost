-- Neon shared demo seed (database: fixna). Run once in Neon SQL Editor.
-- 1. Flyway migrations must have run (first API deploy applies schema).
-- 2. Replace REPLACE_WITH_BCRYPT_HASH below with a BCrypt hash of your demo password.
--    Generate locally: tools\password-tool.cmd hash
-- 3. Skips entirely if owner@example.com already exists.
-- 4. Then run tools/sql/neon-demo-data.sql for campaigns, leads, metrics, etc.
-- App does NOT run this on startup (fixna.test-data.enabled=false on staging).

WITH existing AS (
    SELECT 1 FROM users WHERE email = 'owner@example.com' LIMIT 1
), new_user AS (
    INSERT INTO users (email, password_hash, first_name, last_name)
    SELECT 'owner@example.com', 'REPLACE_WITH_BCRYPT_HASH', 'Demo', 'Owner'
    WHERE NOT EXISTS (SELECT 1 FROM existing)
    RETURNING id
), new_tenant AS (
    INSERT INTO tenants (name, tenant_type)
    SELECT 'Fixna Demo Workspace', 'SMB' FROM new_user
    RETURNING id
), new_membership AS (
    INSERT INTO tenant_memberships (tenant_id, user_id, role)
    SELECT t.id, u.id, 'TENANT_OWNER' FROM new_tenant t CROSS JOIN new_user u
    RETURNING tenant_id
), new_business AS (
    INSERT INTO businesses (tenant_id, name, category, description)
    SELECT tenant_id, 'Demo Neighbourhood Cafe', 'CAFE',
           'Shared demo business for fixna.in'
    FROM new_membership
    RETURNING id, tenant_id
), new_subscription AS (
    INSERT INTO subscriptions (tenant_id, plan_code, status, starts_at)
    SELECT id, 'STARTER', 'ACTIVE', now() FROM new_tenant
    RETURNING id
)
SELECT CASE WHEN EXISTS (SELECT 1 FROM existing) THEN 'skipped: owner@example.com exists'
            WHEN EXISTS (SELECT 1 FROM new_user) THEN 'seeded: owner@example.com'
            ELSE 'no-op' END AS result;
