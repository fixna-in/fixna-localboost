package in.fixna.platform.billing;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import in.fixna.platform.business.BusinessRepository;
import in.fixna.platform.campaign.CampaignRepository;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;

/**
 * Plan-limit enforcement for tenant-scoped creation paths. Resolves the
 * caller's subscription lazily (missing/unknown implies FREE limits) and
 * fails closed with PLAN_* error codes before any entity is persisted.
 */
@Component
public class PlanLimitChecker {

    private final SubscriptionRepository subscriptions;
    private final BusinessRepository businesses;
    private final CampaignRepository campaigns;

    public PlanLimitChecker(
            SubscriptionRepository subscriptions,
            BusinessRepository businesses,
            CampaignRepository campaigns) {
        this.subscriptions = subscriptions;
        this.businesses = businesses;
        this.campaigns = campaigns;
    }

    /** Effective limits for the caller's tenant (FREE when unsubscribed). */
    public PlanLimits limitsForCurrentTenant() {
        UUID tenantId = TenantContext.requireTenantId();
        return subscriptions
                .findByTenantId(tenantId)
                .map(Subscription::effectivePlan)
                .map(PlanLimits::limitsFor)
                .orElseGet(PlanLimits::free);
    }

    /** Throws PLAN_BUSINESS_LIMIT when the tenant already owns the max. */
    public void checkBusinessCreate() {
        UUID tenantId = TenantContext.requireTenantId();
        PlanLimits limits = limitsForCurrentTenant();
        long owned = businesses.findByTenantId(tenantId).size();
        if (owned >= limits.maxBusinesses()) {
            throw new FixnaException(
                    "PLAN_BUSINESS_LIMIT",
                    HttpStatus.FORBIDDEN,
                    "Plan allows " + limits.maxBusinesses() + " businesses");
        }
    }

    /** Throws PLAN_CAMPAIGN_LIMIT when the tenant already owns the max. */
    public void checkCampaignCreate() {
        UUID tenantId = TenantContext.requireTenantId();
        PlanLimits limits = limitsForCurrentTenant();
        long owned = campaigns.findByTenantId(tenantId).size();
        if (owned >= limits.maxCampaigns()) {
            throw new FixnaException(
                    "PLAN_CAMPAIGN_LIMIT",
                    HttpStatus.FORBIDDEN,
                    "Plan allows " + limits.maxCampaigns() + " campaigns");
        }
    }
}
