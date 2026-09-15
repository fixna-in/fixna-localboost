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
