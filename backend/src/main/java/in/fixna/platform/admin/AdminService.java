package in.fixna.platform.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.billing.PlanCode;
import in.fixna.platform.billing.Subscription;
import in.fixna.platform.billing.SubscriptionService;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.tenant.Tenant;
import in.fixna.platform.tenant.TenantMembershipRepository;
import in.fixna.platform.tenant.TenantRepository;
import in.fixna.platform.tenant.TenantType;
import in.fixna.platform.tenant.dto.MembershipResponse;
import in.fixna.platform.user.UserRepository;

/**
 * Platform administration service. Gated by INTERNAL tenant membership —
 * never by a client parameter or a tenant-membership role. Regular tenant
 * members (even TENANT_OWNER) cannot access these operations.
 */
@Service
public class AdminService {

    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final UserRepository users;
    private final SubscriptionService subscriptions;

    public AdminService(
            TenantRepository tenants,
            TenantMembershipRepository memberships,
            UserRepository users,
            SubscriptionService subscriptions) {
        this.tenants = tenants;
        this.memberships = memberships;
        this.users = users;
        this.subscriptions = subscriptions;
    }

    /** Lists tenants for operations support. INTERNAL members only. */
    @Transactional(readOnly = true)
    public List<TenantSummary> listTenants(int page, int size) {
        requirePlatformAdmin();
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        return tenants.findAll(PageRequest.of(safePage, safeSize)).stream()
                .map(TenantSummary::from)
                .toList();
    }

    /** Lists memberships of any tenant. INTERNAL members only. */
    @Transactional(readOnly = true)
    public List<MembershipResponse> listMembers(UUID tenantId) {
        requirePlatformAdmin();
        if (tenantId == null) {
            throw new FixnaException("TENANT_REQUIRED", HttpStatus.BAD_REQUEST, "tenantId is required");
        }
        return memberships.findByTenantId(tenantId).stream()
                .map(m -> MembershipResponse.of(
                        m, users.findById(m.getUserId()).map(u -> u.getEmail()).orElse("unknown")))
                .toList();
    }

    /** Sets any tenant's plan. INTERNAL members only; no payment provider. */
    @Transactional
    public Subscription updatePlan(UUID tenantId, String planCodeRaw) {
        requirePlatformAdmin();
        if (tenantId == null) {
            throw new FixnaException("TENANT_REQUIRED", HttpStatus.BAD_REQUEST, "tenantId is required");
        }
        return subscriptions.updatePlan(tenantId, parsePlanCode(planCodeRaw));
    }

    /** Rejects unknown plan codes instead of silently defaulting. */
    static PlanCode parsePlanCode(String planCodeRaw) {
        if (planCodeRaw == null || planCodeRaw.isBlank()) {
            throw new FixnaException(
                    "PLAN_CODE_REQUIRED", HttpStatus.BAD_REQUEST, "planCode is required");
        }
        try {
            return PlanCode.valueOf(planCodeRaw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new FixnaException(
                    "INVALID_PLAN_CODE",
                    HttpStatus.BAD_REQUEST,
                    "Unknown plan code: " + planCodeRaw.trim());
        }
    }

    /** Fail-closed gate: caller must belong to an INTERNAL tenant. */
    public void requirePlatformAdmin() {
        var tenantId = TenantContext.requireTenantId();
        var tenant = tenants.findById(tenantId)
                .orElseThrow(() -> new FixnaException(
                        "FORBIDDEN", HttpStatus.FORBIDDEN, "Platform admin access required"));
        if (tenant.getTenantType() != TenantType.INTERNAL) {
            throw new FixnaException(
                    "FORBIDDEN", HttpStatus.FORBIDDEN, "Platform admin access required");
        }
    }

    /** Minimal tenant summary for platform admin listings. */
    public record TenantSummary(UUID id, String name) {
        public static TenantSummary from(Tenant tenant) {
            return new TenantSummary(tenant.getId(), tenant.getName());
        }
    }
}
