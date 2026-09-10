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
