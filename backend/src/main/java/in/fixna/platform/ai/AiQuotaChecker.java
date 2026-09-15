package in.fixna.platform.ai;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import in.fixna.platform.billing.PlanLimitChecker;
import in.fixna.platform.common.web.FixnaException;

/**
 * Tenant AI quota gate (WF09): counts {@code ai_usage_log} rows in the
 * trailing 24h window and fails closed with AI_QUOTA_EXCEEDED before the
 * provider is called. Limits come from the tenant's effective plan.
 */
@Component
public class AiQuotaChecker {

    private final AiUsageRepository usage;
    private final PlanLimitChecker plans;

    public AiQuotaChecker(AiUsageRepository usage, PlanLimitChecker plans) {
        this.usage = usage;
        this.plans = plans;
    }

    /** Throws AI_QUOTA_EXCEEDED when the tenant exhausted its daily allowance. */
    public void checkCurrentTenant() {
        UUID tenantId = in.fixna.platform.common.tenant.TenantContext.requireTenantId();
        int max = plans.limitsForCurrentTenant().maxAiRequestsPerDay();
        OffsetDateTime since = OffsetDateTime.now().minusHours(24);
        long used = usage.countByTenantIdAndCreatedAtAfter(tenantId, since);
        if (used >= max) {
            throw new FixnaException(
                    "AI_QUOTA_EXCEEDED",
                    HttpStatus.FORBIDDEN,
                    "AI quota exceeded (" + used + "/" + max + " per 24h)");
        }
    }
}
