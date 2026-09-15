package in.fixna.platform.billing;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.tenant.MembershipRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Subscription service (WF09): lazy FREE materialization on first read and
 * admin plan updates with unknown codes failing closed to FREE.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock SubscriptionRepository subscriptions;

    private SubscriptionService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(subscriptions);
        TenantContext.set(tenantId, userId, MembershipRole.TENANT_OWNER);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void readingMissingSubscriptionMaterializesFree() {
        when(subscriptions.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(subscriptions.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        Subscription result = service.getOrCreateCurrent();

        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.effectivePlan()).isEqualTo(PlanCode.FREE);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void readingExistingSubscriptionReturnsIt() {
        Subscription existing = new Subscription();
        existing.setTenantId(tenantId);
        existing.setPlanCode(PlanCode.STARTER.name());
        existing.setStatus("ACTIVE");
        when(subscriptions.findByTenantId(tenantId)).thenReturn(Optional.of(existing));

        Subscription result = service.getOrCreateCurrent();

        assertThat(result.effectivePlan()).isEqualTo(PlanCode.STARTER);
    }

    @Test
    void updatePlanSwitchesCode() {
        when(subscriptions.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(subscriptions.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        Subscription result = service.updatePlan(tenantId, PlanCode.GROWTH);

        assertThat(result.effectivePlan()).isEqualTo(PlanCode.GROWTH);
    }

    @Test
    void updatePlanWithNullIsFree() {
        when(subscriptions.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(subscriptions.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        Subscription result = service.updatePlan(tenantId, null);

        assertThat(result.effectivePlan()).isEqualTo(PlanCode.FREE);
    }
}