package in.fixna.platform.common.tenant;

import java.util.UUID;

import in.fixna.platform.tenant.MembershipRole;

/**
 * Authenticated request principal resolved by the JWT filter from
 * server-issued claims. Controllers receive it via
 * {@code @AuthenticationPrincipal}; tenant scope always comes from here,
 * never from request parameters.
 */
public record AuthenticatedUser(UUID userId, UUID tenantId, MembershipRole role) {

    public boolean canWrite() {
        return role.canWrite();
    }

    public boolean canAdminister() {
        return role.canAdminister();
    }
}
