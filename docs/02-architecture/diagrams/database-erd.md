# Database Entity Relationship Diagram

PostgreSQL schema as defined by Flyway migrations `V1`–`V9`.
All tenant-owned tables carry `tenant_id` for multi-tenant isolation (shared schema).

> **Note:** `ai_usage_log` (V8) stores tenant/user ids without FK constraints.
> `ai_usage` and `ai_recommendations` (V6) exist in the schema but are not yet
> mapped by JPA entities; runtime AI cost tracking uses `ai_usage_log`.

## Full ERD (relationships)

Relationship-only diagram — renders reliably in GitHub and Cursor Mermaid previews.

```mermaid
erDiagram
    tenants ||--o{ tenant_memberships : "has members"
    users ||--o{ tenant_memberships : "belongs to"
    tenants ||--o{ businesses : "owns"
    businesses ||--o{ business_locations : "has"
    tenants ||--o{ business_locations : "scopes"
    tenants ||--o{ campaigns : "owns"
    businesses ||--o{ campaigns : "runs"
    campaigns ||--o{ campaign_offers : "has"
    campaigns ||--o{ campaign_channels : "allocates"
    campaigns ||--o{ audiences : "targets"
    campaigns ||--o{ geo_targets : "targets"
    campaigns ||--o{ creatives : "has"
    campaigns ||--o{ campaign_metrics : "tracks"
    campaigns ||--o{ ai_recommendations : "receives"
    tenants ||--o{ campaign_offers : "scopes"
    tenants ||--o{ campaign_channels : "scopes"
    tenants ||--o{ audiences : "scopes"
    tenants ||--o{ geo_targets : "scopes"
    tenants ||--o{ creatives : "scopes"
    tenants ||--o{ campaign_metrics : "scopes"
    tenants ||--o{ ai_recommendations : "scopes"
    tenants ||--o{ ai_usage : "scopes"
    tenants ||--o{ platform_connections : "connects"
    tenants ||--o{ leads : "scopes"
    businesses ||--o{ leads : "captures"
    campaigns ||--o{ leads : "attributes"
    tenants ||--|| subscriptions : "has plan"
    tenants ||--o{ audit_logs : "audits"
    users ||--o{ audit_logs : "performs"
    users ||--o{ refresh_tokens : "issues"
    tenants ||--o{ refresh_tokens : "scopes"
```

## Auth and tenancy

```mermaid
erDiagram
    tenants {
        uuid id PK
        string name
        string tenant_type
        datetime created_at
        datetime updated_at
    }
    users {
        uuid id PK
        string email
        string password_hash
        string first_name
        string last_name
        datetime created_at
        datetime updated_at
    }
    tenant_memberships {
        uuid id PK
        uuid tenant_id FK
        uuid user_id FK
        string role
        datetime created_at
    }
    refresh_tokens {
        uuid id PK
        uuid user_id FK
        uuid tenant_id FK
        string token_hash
        datetime expires_at
        string revoked
        datetime created_at
    }
    tenants ||--o{ tenant_memberships : has
    users ||--o{ tenant_memberships : belongs
    users ||--o{ refresh_tokens : issues
    tenants ||--o{ refresh_tokens : scopes
```

## Business and campaigns

```mermaid
erDiagram
    businesses {
        uuid id PK
        uuid tenant_id FK
        string name
        string category
        string description
        string website_url
        string phone
        datetime created_at
        datetime updated_at
    }
    business_locations {
        uuid id PK
        uuid tenant_id FK
        uuid business_id FK
        string address_line
        string city
        string state
        string postal_code
        string country
        float latitude
        float longitude
        datetime created_at
        datetime updated_at
    }
    campaigns {
        uuid id PK
        uuid tenant_id FK
        uuid business_id FK
        string name
        string objective
        string status
        float total_budget
        string currency
        datetime start_at
        datetime end_at
        string external_reference
        datetime created_at
        datetime updated_at
    }
    campaign_offers {
        uuid id PK
        uuid tenant_id FK
        uuid campaign_id FK
        string title
        string description
        string promo_code
        datetime created_at
    }
    campaign_channels {
        uuid id PK
        uuid tenant_id FK
        uuid campaign_id FK
        string channel
        float allocated_budget
        datetime created_at
    }
    tenants ||--o{ businesses : owns
    businesses ||--o{ business_locations : has
    tenants ||--o{ campaigns : owns
    businesses ||--o{ campaigns : runs
    campaigns ||--o{ campaign_offers : has
    campaigns ||--o{ campaign_channels : allocates
```

## Targeting and creatives

```mermaid
erDiagram
    campaigns {
        uuid id PK
        uuid tenant_id FK
        uuid business_id FK
        string name
        string status
    }
    audiences {
        uuid id PK
        uuid tenant_id FK
        uuid campaign_id FK
        string name
        string definition_json
        datetime created_at
    }
    geo_targets {
        uuid id PK
        uuid tenant_id FK
        uuid campaign_id FK
        string target_type
        string name
        float latitude
        float longitude
        float radius_km
        string country_code
        string region_code
        string city
        string postal_code
        datetime created_at
    }
    creatives {
        uuid id PK
        uuid tenant_id FK
        uuid campaign_id FK
        string channel
        string headline
        string creative_body
        string call_to_action
        string status
        datetime created_at
        datetime updated_at
    }
    campaigns ||--o{ audiences : targets
    campaigns ||--o{ geo_targets : targets
    campaigns ||--o{ creatives : has
```

## Platform, analytics, AI and billing

```mermaid
erDiagram
    platform_connections {
        uuid id PK
        uuid tenant_id FK
        string platform
        string external_account_id
        string encrypted_access_token
        string encrypted_refresh_token
        string status
        datetime created_at
        datetime updated_at
    }
    campaign_metrics {
        uuid id PK
        uuid tenant_id FK
        uuid campaign_id FK
        date metric_date
        float spend
        int impressions
        int reach
        int clicks
        int conversions
        int lead_count
        datetime created_at
    }
    leads {
        uuid id PK
        uuid tenant_id FK
        uuid business_id FK
        uuid campaign_id FK
        string name
        string phone
        string email
        string status
        string source
        datetime created_at
        datetime updated_at
    }
    ai_recommendations {
        uuid id PK
        uuid tenant_id FK
        uuid campaign_id FK
        string recommendation_type
        string prompt_version
        string provider
        string model
        string payload_json
        string status
        datetime created_at
    }
    ai_usage {
        uuid id PK
        uuid tenant_id FK
        string provider
        string model
        string request_type
        int input_tokens
        int output_tokens
        float estimated_cost
        string status
        datetime created_at
    }
    ai_usage_log {
        uuid id PK
        uuid tenant_id
        uuid user_id
        string recommendation_type
        string provider
        string model
        string prompt_version
        int input_tokens
        int output_tokens
        float estimated_cost_usd
        int duration_ms
        string status
        datetime created_at
    }
    subscriptions {
        uuid id PK
        uuid tenant_id FK
        string plan_code
        string status
        datetime starts_at
        datetime ends_at
        datetime created_at
        datetime updated_at
    }
    audit_logs {
        uuid id PK
        uuid tenant_id FK
        uuid user_id FK
        string action
        string resource_type
        uuid resource_id
        string metadata_json
        datetime created_at
    }
    tenants ||--o{ platform_connections : connects
    campaigns ||--o{ campaign_metrics : tracks
    tenants ||--o{ leads : scopes
    businesses ||--o{ leads : captures
    campaigns ||--o{ leads : attributes
    campaigns ||--o{ ai_recommendations : receives
    tenants ||--o{ ai_usage : scopes
    tenants ||--|| subscriptions : plan
    tenants ||--o{ audit_logs : audits
    users ||--o{ audit_logs : performs
```

## Domain clusters

```mermaid
flowchart TB
    subgraph auth["Auth and tenancy"]
        tenants
        users
        tenant_memberships
        refresh_tokens
    end

    subgraph business["Business profile"]
        businesses
        business_locations
    end

    subgraph campaign["Campaign execution"]
        campaigns
        campaign_offers
        campaign_channels
        audiences
        geo_targets
        creatives
    end

    subgraph platform["Platform and analytics"]
        platform_connections
        campaign_metrics
        leads
    end

    subgraph ai["AI and billing"]
        ai_recommendations
        ai_usage
        ai_usage_log
        subscriptions
    end

    subgraph ops["Operations"]
        audit_logs
    end

    tenants --> tenant_memberships
    users --> tenant_memberships
    tenants --> businesses
    businesses --> business_locations
    tenants --> campaigns
    businesses --> campaigns
    campaigns --> campaign_offers
    campaigns --> campaign_channels
    campaigns --> audiences
    campaigns --> geo_targets
    campaigns --> creatives
    tenants --> platform_connections
    campaigns --> campaign_metrics
    tenants --> leads
    businesses --> leads
    campaigns --> leads
    campaigns --> ai_recommendations
    tenants --> subscriptions
    tenants --> audit_logs
    users --> refresh_tokens
```

## Tenancy model

```mermaid
flowchart LR
    tenant[tenants] --> membership[tenant_memberships]
    user[users] --> membership
    membership --> business[businesses]
    business --> location[business_locations]
    business --> campaign[campaigns]
    campaign --> child["audiences, geo_targets, creatives, campaign_channels, campaign_offers, campaign_metrics, ai_recommendations"]
    tenant --> subscription[subscriptions]
    tenant --> platform[platform_connections]
    tenant --> lead[leads]
```

## Key constraints

| Table | Constraint |
|-------|------------|
| `tenant_memberships` | `UNIQUE (tenant_id, user_id)` |
| `campaign_channels` | `UNIQUE (campaign_id, channel)` |
| `campaign_metrics` | `UNIQUE (campaign_id, metric_date)` |
| `subscriptions` | `UNIQUE (tenant_id)` — one plan per tenant |
| `users.email` | `UNIQUE` |
| `refresh_tokens.token_hash` | `UNIQUE` |
| `campaigns.total_budget` | `CHECK (total_budget > 0)` |
| `campaign_channels.allocated_budget` | `CHECK (allocated_budget >= 0)` |

## Migration index

| Migration | Tables |
|-----------|--------|
| V1 | `tenants`, `users`, `tenant_memberships` |
| V2 | `businesses`, `business_locations` |
| V3 | `campaigns`, `campaign_offers` |
| V4 | `audiences`, `geo_targets`, `creatives`, `campaign_channels` |
| V5 | `platform_connections`, `campaign_metrics`, `leads` |
| V6 | `ai_recommendations`, `ai_usage`, `subscriptions`, `audit_logs` |
| V7 | `refresh_tokens` |
| V8 | `ai_usage_log` |
| V9 | indexes only (no new tables) |

## Diagram notes

Mermaid `erDiagram` attribute blocks are sensitive to reserved words and compound
key markers. This file avoids:

- `body` (reserved) — shown as `creative_body`
- `jsonb` columns — shown as `*_json` string fields
- `FK_UK` / `UK` markers — uniqueness documented in the constraints table
- oversized single diagrams — split by domain for reliable rendering
