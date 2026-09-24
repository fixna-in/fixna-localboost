package in.fixna.platform.tenant;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.common.audit.AuditEvent;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.logging.LoggingConstants;
import in.fixna.platform.common.logging.LoggingContext;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.tenant.dto.MembershipResponse;
import in.fixna.platform.tenant.dto.TenantResponse;
import in.fixna.platform.user.UserRepository;

/**
 * Tenant application service. US-002/US-003: current-tenant view, tenant
 * creation (extra tenant for the caller), member listing (admin-only),
 * member removal (admin-only, owner protected).
 */
@Service
public class TenantService {

    private static final Logger LOG = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final UserRepository users;
    private final AuditPublisher audit;

    public TenantService(
            TenantRepository tenants,
            TenantMembershipRepository memberships,
            UserRepository users,
            AuditPublisher audit) {
        this.tenants = tenants;
        this.memberships = memberships;
        this.users = users;
        this.audit = audit;
    }

    /** Returns the caller's current tenant (scope from JWT, never params). */
    @Transactional(readOnly = true)
    public TenantResponse current() {
        UUID tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenants.findById(tenantId)
                .orElseThrow(() -> new FixnaException(
                        "TENANT_NOT_FOUND", HttpStatus.NOT_FOUND, "Tenant not found"));
        return TenantResponse.from(tenant);
    }

    /** Creates an additional tenant; caller becomes its owner. */
    @Transactional
    public TenantResponse create(String name) {
        UUID userId = TenantContext.requireUserId();
        if (name == null || name.isBlank()) {
            throw new FixnaException(
                    "VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "Tenant name is required");
        }
        Tenant tenant = new Tenant();
        tenant.setName(name.trim());
        tenant.setTenantType(TenantType.SMB);
        tenants.save(tenant);

        TenantMembership membership = new TenantMembership();
        membership.setTenantId(tenant.getId());
        membership.setUserId(userId);
        membership.setRole(MembershipRole.TENANT_OWNER);
        memberships.save(membership);

        LOG.info("Tenant created tenantId={} userId={}", tenant.getId(), userId);
        audit.publish(new AuditEvent(
                "tenant.created", tenant.getId(), userId, "tenant", tenant.getId().toString(),
                Map.of(), null));
        return TenantResponse.from(tenant);
    }

    /** Lists members of the caller's tenant. Admin-only. */
    @Transactional(readOnly = true)
    public List<MembershipResponse> members() {
        TenantContext.requireAdmin();
        UUID tenantId = TenantContext.requireTenantId();
        return memberships.findByTenantId(tenantId).stream()
                .map(m -> MembershipResponse.of(
                        m, users.findById(m.getUserId()).map(u -> u.getEmail()).orElse("unknown")))
                .toList();
    }

    /** Removes a member. Admin-only; the last owner cannot be removed. */
    @Transactional
    public void removeMember(UUID memberUserId) {
        TenantContext.requireAdmin();
        UUID tenantId = TenantContext.requireTenantId();
        TenantMembership membership = memberships
                .findByTenantIdAndUserId(tenantId, memberUserId)
                .orElseThrow(() -> new FixnaException(
                        "MEMBERSHIP_NOT_FOUND", HttpStatus.NOT_FOUND, "Membership not found"));
        if (membership.getRole() == MembershipRole.TENANT_OWNER) {
            long owners = memberships.findByTenantId(tenantId).stream()
                    .filter(m -> m.getRole() == MembershipRole.TENANT_OWNER)
                    .count();
            if (owners <= 1) {
                throw new FixnaException(
                        "LAST_OWNER", HttpStatus.CONFLICT, "Cannot remove the last owner");
            }
        }
        memberships.delete(membership);
        audit.publish(new AuditEvent(
                "tenant.member_removed", tenantId, TenantContext.requireUserId(), "membership",
                memberUserId.toString(), Map.of(), null));
    }
}
