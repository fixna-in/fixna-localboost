-- Local test fixture only; executed by LocalTestDataSeeder after Flyway.
-- :passwordHash is bound by JDBC, never interpolated or stored as plaintext.
-- All rows are inserted atomically by this single PostgreSQL statement.
-- An existing email skips the entire fixture, including memberships.
WITH new_user AS (
    INSERT INTO users (email, password_hash, first_name, last_name)
    VALUES ('owner@example.com', :passwordHash, 'Test', 'Owner')
    ON CONFLICT (email) DO NOTHING
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
           'Fictional local test business. Not a real advertiser.'
    FROM new_membership
    RETURNING id, tenant_id
), new_location AS (
    INSERT INTO business_locations
        (tenant_id, business_id, address_line, city, state, postal_code, country)
    SELECT tenant_id, id, 'Demo location, Sector 18', 'Noida',
           'Uttar Pradesh', '201301', 'India'
    FROM new_business
    RETURNING id
), new_subscription AS (
    INSERT INTO subscriptions (tenant_id, plan_code, status, starts_at)
    SELECT id, 'STARTER', 'ACTIVE', now() FROM new_tenant
    RETURNING id
), new_campaigns AS (
    INSERT INTO campaigns
        (tenant_id, business_id, name, objective, status, total_budget, currency, start_at, end_at)
    SELECT b.tenant_id, b.id, v.name, v.objective, v.status, v.budget, 'INR',
        CASE WHEN v.status = 'COMPLETED' THEN now() - interval '8 days' ELSE now() + interval '1 day' END,
        CASE WHEN v.status = 'COMPLETED' THEN now() - interval '1 day' ELSE now() + interval '8 days' END
    FROM new_business b CROSS JOIN (VALUES
        ('DEMO - Cafe opening results (synthetic)', 'LEAD_GENERATION', 'COMPLETED', 7000.00),
        ('DEMO - Weekend coffee offer', 'STORE_VISITS', 'DRAFT', 3500.00)
    ) AS v(name, objective, status, budget)
    RETURNING id, tenant_id, business_id, status, total_budget
), new_offers AS (
    INSERT INTO campaign_offers (tenant_id, campaign_id, title, description, promo_code)
    SELECT tenant_id, id, 'Demo coffee and snack combo',
        'Fictional promotion for local testing only.', 'DEMOCOFFEE' FROM new_campaigns
    RETURNING id
), new_channels AS (
    INSERT INTO campaign_channels (tenant_id, campaign_id, channel, allocated_budget)
    SELECT c.tenant_id, c.id, v.channel, c.total_budget / 2
    FROM new_campaigns c CROSS JOIN (VALUES ('GOOGLE_ADS'), ('META_ADS')) AS v(channel)
    RETURNING id
), new_geo AS (
    INSERT INTO geo_targets (tenant_id, campaign_id, target_type, name, latitude, longitude,
        radius_km, country_code, city)
    SELECT tenant_id, id, 'RADIUS', 'Demo Sector 18 catchment', 28.5700, 77.3200, 3, 'IN', 'Noida'
    FROM new_campaigns
    RETURNING id
), new_audiences AS (
    INSERT INTO audiences (tenant_id, campaign_id, name, definition)
    SELECT tenant_id, id, 'Demo local coffee enthusiasts',
        CAST('{"ageMin":21,"ageMax":55,"interests":["coffee","cafes"]}' AS jsonb)
    FROM new_campaigns
    RETURNING id
), new_creatives AS (
    INSERT INTO creatives (tenant_id, campaign_id, channel, headline, body, call_to_action, status)
    SELECT c.tenant_id, c.id, v.channel, 'DEMO - Your neighbourhood coffee break',
        'Fictional creative for testing. Not a live advertisement.', 'Learn more', 'DRAFT'
    FROM new_campaigns c CROSS JOIN (VALUES ('GOOGLE_ADS'), ('META_ADS')) AS v(channel)
    RETURNING id
), new_leads AS (
    INSERT INTO leads (tenant_id, business_id, campaign_id, name, email, status, source, created_at)
    SELECT c.tenant_id, c.business_id, c.id, 'Demo Customer ' || n,
        'demo.customer.' || n || '@example.com',
        CASE WHEN n <= 3 THEN 'NEW' WHEN n <= 5 THEN 'CONTACTED'
             WHEN n <= 7 THEN 'QUALIFIED' WHEN n <= 9 THEN 'CONVERTED' ELSE 'LOST' END,
        'LOCAL_DEMO_SYNTHETIC', now() - interval '1 day'
    FROM new_campaigns c CROSS JOIN generate_series(1, 10) AS n
    WHERE c.status = 'COMPLETED'
    RETURNING id
), new_metrics AS (
    INSERT INTO campaign_metrics (tenant_id, campaign_id, metric_date, spend, impressions,
        reach, clicks, conversions, leads)
    SELECT c.tenant_id, c.id, current_date - n, 350.00, 2000, 1500, 100,
        CASE WHEN n = 1 THEN 2 ELSE 0 END,
        CASE WHEN n = 1 THEN 10 ELSE 0 END
    FROM new_campaigns c CROSS JOIN generate_series(1, 7) AS n
    WHERE c.status = 'COMPLETED'
    RETURNING id
)
SELECT count(*) FROM new_location;
