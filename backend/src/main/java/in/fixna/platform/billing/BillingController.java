package in.fixna.platform.billing;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Subscription endpoints. Tenant scope comes from the JWT; the response
 * carries the effective plan plus limits — no payment state (out of scope).
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "subscriptions", description = "Current tenant plan and limits")
public class BillingController {

    private final SubscriptionService subscriptions;
    private final PlanLimitChecker limits;

    public BillingController(SubscriptionService subscriptions, PlanLimitChecker limits) {
        this.subscriptions = subscriptions;
        this.limits = limits;
    }

    @Operation(summary = "Get current tenant subscription and effective limits")
    @GetMapping("/current")
    public ResponseEntity<SubscriptionResponse> current() {
        Subscription subscription = subscriptions.getOrCreateCurrent();
        PlanLimits effective = limits.limitsForCurrentTenant();
        return ResponseEntity.ok(SubscriptionResponse.from(subscription, effective));
    }

    /** Minimal subscription view — plan, status and effective limits. */
    public record SubscriptionResponse(
            UUID tenantId, String planCode, String status, PlanLimits effectiveLimits) {
        static SubscriptionResponse from(Subscription subscription, PlanLimits effective) {
            return new SubscriptionResponse(
                    subscription.getTenantId(),
                    subscription.effectivePlan().name(),
                    subscription.getStatus(),
                    effective);
        }
    }
}
