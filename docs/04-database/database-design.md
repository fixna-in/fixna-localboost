# Database Design

Core tables:
tenants, users, tenant_memberships, businesses, business_locations,
platform_connections, campaigns, campaign_offers, audiences, geo_targets,
creatives, campaign_channels, campaign_metrics, leads, ai_recommendations,
ai_usage, subscriptions, audit_logs.

Tenant-owned records contain tenant_id.
Use UUID IDs and timestamptz timestamps.
