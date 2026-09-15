package in.fixna.platform.tenant.dto;

import java.util.UUID;

import in.fixna.platform.tenant.TenantMembership;

/** Membership response. Email is resolved by the service layer. */
public record MembershipResponse(UUID userId, String email, String role) {

    public static MembershipResponse of(TenantMembership membership, String email) {
        return new MembershipResponse(membership.getUserId(), email, membership.getRole().name());
    }
}
