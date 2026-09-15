package in.fixna.platform.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import in.fixna.platform.tenant.MembershipRole;
import in.fixna.platform.tenant.TenantMembershipRepository;

/** Server-side membership resolution for request authentication. */
@Component
public class MembershipLookup {

    private final TenantMembershipRepository memberships;

    public MembershipLookup(TenantMembershipRepository memberships) {
        this.memberships = memberships;
    }

    public Optional<MembershipRole> roleFor(UUID tenantId, UUID userId) {
        return memberships.findByTenantIdAndUserId(tenantId, userId).map(m -> m.getRole());
    }
}
