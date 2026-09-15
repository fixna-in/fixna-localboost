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

