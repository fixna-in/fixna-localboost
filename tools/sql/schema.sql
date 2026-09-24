-- LOCAL ONLY. Run with psql -X -h localhost -U postgres -d localboost -f schema.sql
-- Snapshot of immutable V1-V9 migrations. Empty public schema required.
-- Do not run on an existing migrated database. Future changes use Flyway.
BEGIN;
DO $$ BEGIN
  IF inet_server_addr() IS NOT NULL AND inet_server_addr() NOT IN ('127.0.0.1'::inet, '::1'::inet) THEN
    RAISE EXCEPTION 'Local PostgreSQL only';
  END IF;
  IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public') THEN
    RAISE EXCEPTION 'Public schema is not empty; use Flyway for existing databases';
  END IF;
END $$;
SET LOCAL search_path = public;

-- Source: V1__create_tenants_users.sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    tenant_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tenant_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_membership_user ON tenant_memberships(user_id);
CREATE INDEX idx_membership_tenant ON tenant_memberships(tenant_id);

-- Source: V2__create_businesses.sql
CREATE TABLE businesses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    description TEXT,
    website_url VARCHAR(1000),
    phone VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE business_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    business_id UUID NOT NULL REFERENCES businesses(id),
    address_line VARCHAR(500),
    city VARCHAR(150),
    state VARCHAR(150),
    postal_code VARCHAR(30),
    country VARCHAR(100) DEFAULT 'India',
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_business_tenant ON businesses(tenant_id);
CREATE INDEX idx_location_tenant ON business_locations(tenant_id);
CREATE INDEX idx_location_business ON business_locations(business_id);

-- Source: V3__create_campaigns.sql
CREATE TABLE campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    business_id UUID NOT NULL REFERENCES businesses(id),
    name VARCHAR(255) NOT NULL,
    objective VARCHAR(80) NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_budget NUMERIC(14,2) NOT NULL CHECK (total_budget > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    external_reference VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE campaign_offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    campaign_id UUID NOT NULL REFERENCES campaigns(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    promo_code VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_campaign_tenant ON campaigns(tenant_id);
CREATE INDEX idx_campaign_business ON campaigns(business_id);
CREATE INDEX idx_offer_tenant ON campaign_offers(tenant_id);

-- Source: V4__create_targeting_creatives.sql
CREATE TABLE audiences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    campaign_id UUID NOT NULL REFERENCES campaigns(id),
    name VARCHAR(255) NOT NULL,
    definition JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE geo_targets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    campaign_id UUID NOT NULL REFERENCES campaigns(id),
    target_type VARCHAR(30) NOT NULL,
    name VARCHAR(255),
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    radius_km NUMERIC(8,2),
    country_code VARCHAR(10),
    region_code VARCHAR(100),
    city VARCHAR(150),
    postal_code VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE creatives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    campaign_id UUID NOT NULL REFERENCES campaigns(id),
    channel VARCHAR(30) NOT NULL,
    headline VARCHAR(500),
    body TEXT,
    call_to_action VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE campaign_channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    campaign_id UUID NOT NULL REFERENCES campaigns(id),
    channel VARCHAR(30) NOT NULL,
    allocated_budget NUMERIC(14,2) NOT NULL CHECK (allocated_budget >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(campaign_id, channel)
);

CREATE INDEX idx_audience_tenant ON audiences(tenant_id);
CREATE INDEX idx_geo_tenant ON geo_targets(tenant_id);
CREATE INDEX idx_creative_tenant ON creatives(tenant_id);

-- Source: V5__create_platform_analytics_leads.sql
CREATE TABLE platform_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    platform VARCHAR(30) NOT NULL,
    external_account_id VARCHAR(255),
    encrypted_access_token TEXT,
    encrypted_refresh_token TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'DISCONNECTED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE campaign_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    campaign_id UUID NOT NULL REFERENCES campaigns(id),
    metric_date DATE NOT NULL,
    spend NUMERIC(14,2) NOT NULL DEFAULT 0,
    impressions BIGINT NOT NULL DEFAULT 0,
    reach BIGINT NOT NULL DEFAULT 0,
    clicks BIGINT NOT NULL DEFAULT 0,
    conversions BIGINT NOT NULL DEFAULT 0,
    leads BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(campaign_id, metric_date)
);

CREATE TABLE leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    business_id UUID NOT NULL REFERENCES businesses(id),
    campaign_id UUID REFERENCES campaigns(id),
    name VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(320),
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    source VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_metrics_tenant_campaign ON campaign_metrics(tenant_id, campaign_id);
CREATE INDEX idx_leads_tenant ON leads(tenant_id);

-- Source: V6__create_ai_billing_audit.sql
CREATE TABLE ai_recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    campaign_id UUID NOT NULL REFERENCES campaigns(id),
    recommendation_type VARCHAR(50) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100),
    payload JSONB NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'GENERATED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ai_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100),
    request_type VARCHAR(100) NOT NULL,
    input_tokens BIGINT DEFAULT 0,
    output_tokens BIGINT DEFAULT 0,
    estimated_cost NUMERIC(14,6) DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE REFERENCES tenants(id),
    plan_code VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id UUID,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_usage_tenant ON ai_usage(tenant_id);
CREATE INDEX idx_audit_tenant_time ON audit_logs(tenant_id, created_at);

-- Source: V7__create_refresh_tokens.sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tenant ON refresh_tokens(tenant_id);
CREATE INDEX idx_refresh_expires ON refresh_tokens(expires_at);

-- Source: V8__create_ai_usage_log.sql
-- Workflow 06: AI usage/cost tracking. ai_usage_log is owned by this migration;
-- V6's ai_usage (recommendation history) is untouched.
CREATE TABLE ai_usage_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    recommendation_type VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100),
    prompt_version VARCHAR(20) NOT NULL,
    input_tokens INTEGER,
    output_tokens INTEGER,
    estimated_cost_usd NUMERIC(10,6),
    duration_ms BIGINT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_usage_log_tenant ON ai_usage_log(tenant_id);

-- Source: V9__plans_and_ai_quota.sql
-- Workflow 09: persistent audit trail + plan quota indexes.
-- Canonical tables are V6 (audit_logs) and V8 (ai_usage_log); V6 also owns
-- subscriptions. This migration only adds viewer/query indexes.
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_created
    ON audit_logs(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_usage_log_tenant_created
    ON ai_usage_log(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_subscriptions_tenant
    ON subscriptions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_businesses_tenant
    ON businesses(tenant_id);
CREATE INDEX IF NOT EXISTS idx_campaigns_tenant
    ON campaigns(tenant_id);


-- Equivalent to a Flyway baseline at V9, not fabricated migration checksums.
CREATE TABLE public.flyway_schema_history (
 installed_rank integer NOT NULL PRIMARY KEY, version varchar(50),
 description varchar(200) NOT NULL, type varchar(20) NOT NULL,
 script varchar(1000) NOT NULL, checksum integer, installed_by varchar(100) NOT NULL,
 installed_on timestamp NOT NULL DEFAULT now(), execution_time integer NOT NULL,
 success boolean NOT NULL
);
CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history(success);
INSERT INTO public.flyway_schema_history
 (installed_rank, version, description, type, script, installed_by, execution_time, success)
 VALUES (1, '9', 'Local schema snapshot V1-V9', 'BASELINE', 'Local schema snapshot V1-V9', current_user, 0, true);
COMMIT;