package in.fixna.platform.billing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import in.fixna.platform.business.Business;
import in.fixna.platform.business.BusinessRepository;
import in.fixna.platform.campaign.Campaign;
import in.fixna.platform.campaign.CampaignRepository;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.tenant.MembershipRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Plan-limit enforcement (WF09, BACKLOG Platform/Billing): FREE tenants get
 * one business and one campaign; exceeding counts fail closed with PLAN_*.
 */
@ExtendWith(MockitoExtension.class)
class PlanLimitCheckerTest {

    @Mock SubscriptionRepository subscriptions;
    @Mock BusinessRepository businesses;
    @Mock CampaignRepository campaigns;

    private PlanLimitChecker checker;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        checker = new PlanLimitChecker(subscriptions, businesses, campaigns);
        TenantContext.set(tenantId, userId, MembershipRole.TENANT_OWNER);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Subscription subscription(PlanCode plan) {
        Subscription subscription = new Subscription();
        subscription.setTenantId(tenantId);
        subscription.setPlanCode(plan.name());
        subscription.setStatus("ACTIVE");
        return subscription;
    }

    private Business business() {
        Business business = new Business();
        business.setId(UUID.randomUUID());
        business.setTenantId(tenantId);
        return business;
    }

    private Campaign campaign() {
        Campaign campaign = new Campaign();
        campaign.setId(UUID.randomUUID());
        campaign.setTenantId(tenantId);
        return campaign;
    }

    @Test
    void freePlanFailsWhenBusinessLimitReached() {
        when(subscriptions.findByTenantId(tenantId)).thenReturn(Optional.of(subscription(PlanCode.FREE)));
        when(businesses.findByTenantId(tenantId)).thenReturn(List.of(business()));

        assertThatThrownBy(() -> checker.checkBusinessCreate())
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("PLAN_BUSINESS_LIMIT");
    }

    @Test
    void growthPlanAllowsMoreBusinesses() {
        when(subscriptions.findByTenantId(tenantId)).thenReturn(Optional.of(subscription(PlanCode.GROWTH)));
        when(businesses.findByTenantId(tenantId)).thenReturn(List.of());

        checker.checkBusinessCreate(); // 0/10 — must not throw
    }

    @Test
    void freePlanFailsWhenCampaignLimitReached() {
        when(subscriptions.findByTenantId(tenantId)).thenReturn(Optional.of(subscription(PlanCode.FREE)));
        when(campaigns.findByTenantId(tenantId)).thenReturn(List.of(campaign()));

        assertThatThrownBy(() -> checker.checkCampaignCreate())
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("PLAN_CAMPAIGN_LIMIT");
    }

    @Test
    void missingSubscriptionFailsClosedToFreeLimits() {
        when(subscriptions.findByTenantId(tenantId)).thenReturn(Optional.empty());
        // FREE maxBusinesses = 1; mock returns 1 -> limit reached for FREE.
        when(businesses.findByTenantId(tenantId)).thenReturn(List.of(business()));

        assertThatThrownBy(() -> checker.checkBusinessCreate())
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("PLAN_BUSINESS_LIMIT");
    }

    @Test
    void limitsForCurrentTenantReflectEffectivePlan() {
        when(subscriptions.findByTenantId(tenantId)).thenReturn(Optional.of(subscription(PlanCode.STARTER)));

        PlanLimits limits = checker.limitsForCurrentTenant();

        assertThat(limits.maxBusinesses()).isEqualTo(3);
        assertThat(limits.maxCampaigns()).isEqualTo(5);
        assertThat(limits.maxAiRequestsPerDay()).isEqualTo(100);
    }
}