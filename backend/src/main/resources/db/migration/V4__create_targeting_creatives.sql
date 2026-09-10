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
