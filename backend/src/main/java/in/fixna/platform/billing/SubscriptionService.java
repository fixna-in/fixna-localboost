package in.fixna.platform.billing;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.common.tenant.TenantContext;

/**
 * Subscription reader/writer (WF09). Every tenant lazily owns exactly one
 * subscription row; missing rows materialize as FREE without a payment
 * provider (explicitly excluded). Plan changes are admin-only operations
 * applied by {@code AdminService} via {@link #updatePlan}.
 */
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptions;

    public SubscriptionService(SubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    /** Current tenant's subscription, creating a FREE row on first read. */
    @Transactional
    public Subscription getOrCreateCurrent() {
        UUID tenantId = TenantContext.requireTenantId();
        return subscriptions.findByTenantId(tenantId).orElseGet(() -> {
            Subscription fresh = new Subscription();
            fresh.setTenantId(tenantId);
            fresh.setPlanCode(PlanCode.FREE.name());
            fresh.setStatus("ACTIVE");
            return subscriptions.save(fresh);
        });
    }

    /** Sets the plan for any tenant. Caller must have passed platform-admin gate. */
    @Transactional
    public Subscription updatePlan(UUID tenantId, PlanCode plan) {
        Subscription subscription = subscriptions.findByTenantId(tenantId).orElseGet(() -> {
            Subscription fresh = new Subscription();
            fresh.setTenantId(tenantId);
            fresh.setStatus("ACTIVE");
            return fresh;
        });
        subscription.setPlanCode((plan == null ? PlanCode.FREE : plan).name());
        if (subscription.getStatus() == null) {
            subscription.setStatus("ACTIVE");
        }
        return subscriptions.save(subscription);
    }
}
