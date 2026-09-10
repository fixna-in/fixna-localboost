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
