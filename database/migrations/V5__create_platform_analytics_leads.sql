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
